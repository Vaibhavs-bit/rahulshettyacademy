// Local-only dev server for the Test Case Tracker.
//
// Why this exists: the tracker is normally a plain static page (no server needed) so it
// keeps working when opened as a file:// URL. But viewing/editing the *actual* Page Object
// (POM) and TestNG source files - so you can tweak a locator/xpath and have it land in the
// real .java file - needs filesystem access that a browser page can't do on its own. This
// server adds that filesystem bridge, scoped strictly to this project's src/ folders.
//
// Run with:  node server.js   (from tools/testcase-tracker), then open http://localhost:5500
const http = require("http");
const fs = require("fs");
const path = require("path");
const url = require("url");
const { spawn } = require("child_process");

const PORT = process.env.PORT || 5500;
const HOST = "127.0.0.1"; // local-only: never bind 0.0.0.0, this server can write source files and run Maven
const TRACKER_ROOT = __dirname;
const PROJECT_ROOT = path.resolve(__dirname, "..", "..");

// Only these directories may be listed/read/written - prevents path traversal to arbitrary files.
const ALLOWED_SOURCE_ROOTS = [
  path.join(PROJECT_ROOT, "src", "main", "java"),
  path.join(PROJECT_ROOT, "src", "test", "java")
];
const PAGES_DIR = path.join(PROJECT_ROOT, "src", "main", "java", "com", "automation", "pages");
const TESTS_DIR = path.join(PROJECT_ROOT, "src", "test", "java", "com", "automation", "tests");
const MAX_BODY_BYTES = 2 * 1024 * 1024; // 2MB cap on source file writes
const EXECUTION_DATA_FILE = path.join(TRACKER_ROOT, "data", "execution-data.js");
const RUN_TIMEOUT_MS = 10 * 60 * 1000; // kill a run that hangs past 10 minutes
const ALLOWED_BROWSERS = new Set(["edge", "chrome", "firefox"]);
const AUTOMATION_KEY_PATTERN = /^[A-Za-z_][\w.]*#[A-Za-z_]\w*$/;

/** Locates mvn.cmd: manual installs on this machine aren't on PATH by default (see repo README). */
function findMavenExecutable() {
  if (process.env.MAVEN_HOME) {
    const p = path.join(process.env.MAVEN_HOME, "bin", "mvn.cmd");
    if (fs.existsSync(p)) return p;
  }
  const toolsDir = "C:\\Tools";
  if (fs.existsSync(toolsDir)) {
    const match = fs.readdirSync(toolsDir).find(n => /^apache-maven-/.test(n));
    if (match) {
      const p = path.join(toolsDir, match, "bin", "mvn.cmd");
      if (fs.existsSync(p)) return p;
    }
  }
  return "mvn.cmd"; // fall back to whatever is already on PATH
}

const MIME = {
  ".html": "text/html; charset=utf-8",
  ".js": "application/javascript; charset=utf-8",
  ".css": "text/css; charset=utf-8",
  ".json": "application/json; charset=utf-8"
};

/** Resolves a user-supplied relative path and verifies it stays inside an allowed src/ root with a .java extension. */
function resolveSafeJavaPath(relativePath) {
  if (typeof relativePath !== "string" || !relativePath.trim()) return null;
  if (path.extname(relativePath) !== ".java") return null;
  const normalized = relativePath.replace(/\\/g, "/").replace(/^\/+/, "");
  const resolved = path.resolve(PROJECT_ROOT, normalized);
  const withinAllowedRoot = ALLOWED_SOURCE_ROOTS.some(root => {
    const rel = path.relative(root, resolved);
    return rel && !rel.startsWith("..") && !path.isAbsolute(rel);
  });
  return withinAllowedRoot ? resolved : null;
}

function classNameFromFile(filePath) {
  return path.basename(filePath, ".java");
}

function fqcnFromJavaFile(absPath, content) {
  const pkgMatch = content.match(/^\s*package\s+([\w.]+)\s*;/m);
  const pkg = pkgMatch ? pkgMatch[1] : "";
  const cls = classNameFromFile(absPath);
  return pkg ? `${pkg}.${cls}` : cls;
}

function listJavaFiles(dir) {
  if (!fs.existsSync(dir)) return [];
  return fs.readdirSync(dir, { withFileTypes: true })
    .filter(e => e.isFile() && e.name.endsWith(".java"))
    .map(e => {
      const abs = path.join(dir, e.name);
      const content = fs.readFileSync(abs, "utf8");
      return {
        relativePath: path.relative(PROJECT_ROOT, abs).replace(/\\/g, "/"),
        className: classNameFromFile(abs),
        fqcn: fqcnFromJavaFile(abs, content)
      };
    });
}

function sendJson(res, status, obj) {
  const body = JSON.stringify(obj);
  res.writeHead(status, { "Content-Type": "application/json; charset=utf-8", "Content-Length": Buffer.byteLength(body) });
  res.end(body);
}

function readBody(req, cb) {
  let size = 0;
  const chunks = [];
  req.on("data", chunk => {
    size += chunk.length;
    if (size > MAX_BODY_BYTES) {
      req.destroy();
      cb(new Error("Request body too large"));
      return;
    }
    chunks.push(chunk);
  });
  req.on("end", () => {
    let parsed;
    try {
      const raw = Buffer.concat(chunks).toString("utf8");
      parsed = raw ? JSON.parse(raw) : {};
    } catch (e) {
      cb(new Error("Invalid JSON body"));
      return;
    }
    cb(null, parsed); // outside the try/catch so bugs in the handler aren't mislabeled as parse errors
  });
  req.on("error", cb);
}

function pageBoilerplate(className) {
  return `package com.automation.pages;

import com.automation.base.BasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * Page object for ${className.replace(/Page$/, "")}.
 */
public class ${className} extends BasePage {

    // TODO: replace with real locators/xpaths for this page
    private final By exampleElement = By.xpath("//*[@id='example']");

    public ${className}(WebDriver driver) {
        super(driver);
    }

    public boolean isDisplayed() {
        return actions.isDisplayed(exampleElement);
    }
}
`;
}

function testBoilerplate(className) {
  return `package com.automation.tests;

import org.testng.Assert;
import org.testng.annotations.Test;

public class ${className} extends BaseTest {

    @Test(priority = 1, groups = {"regression"}, description = "TODO: describe this scenario")
    public void verifyExample() {
        logger.info("TODO: implement test steps");
        Assert.assertTrue(true, "Replace with a real assertion");
    }
}
`;
}

/** Groups "fqcn#method" automation keys by class, for a multi-class TestNG suite. */
function groupAutomationKeysByClass(automationKeys) {
  const byClass = new Map();
  for (const key of automationKeys) {
    const [fqcn, method] = key.split("#");
    if (!byClass.has(fqcn)) byClass.set(fqcn, new Set());
    byClass.get(fqcn).add(method);
  }
  return byClass;
}

/** Builds a one-off TestNG suite (with this project's TestListener) covering exactly the given automation keys. */
function buildSuiteXml(automationKeys) {
  const byClass = groupAutomationKeysByClass(automationKeys);
  const classesXml = [...byClass.entries()].map(([fqcn, methods]) => `
      <class name="${fqcn}">
        <methods>
          ${[...methods].map(m => `<include name="${m}"/>`).join("\n          ")}
        </methods>
      </class>`).join("");
  const parallelAttrs = byClass.size > 1 ? ' parallel="classes" thread-count="2"' : "";
  return `<!DOCTYPE suite SYSTEM "https://testng.org/testng-1.0.dtd">
<suite name="TrackerRunSuite"${parallelAttrs}>
  <listeners>
    <listener class-name="com.automation.listeners.TestListener"/>
  </listeners>
  <test name="TrackerRun">
    <classes>${classesXml}
    </classes>
  </test>
</suite>
`;
}

/** Runs `mvn test` against a generated suite file and responds with the captured log + exit code. */
function runMavenSuite(res, suiteXml, browser, headless) {
  const tempSuitePath = path.join(PROJECT_ROOT, "target", `tracker-run-${process.pid}-${Date.now()}.xml`);
  fs.mkdirSync(path.dirname(tempSuitePath), { recursive: true });
  fs.writeFileSync(tempSuitePath, suiteXml, "utf8");

  const mavenArgs = [
    "-q",
    `-Dsurefire.suiteXmlFiles=${tempSuitePath}`,
    `-Dbrowser=${browser}`,
    `-Dheadless=${headless}`,
    "test"
  ];

  const mvnExe = findMavenExecutable();
  // mvn.cmd is a batch file - Windows can't exec it directly without a shell (spawn EINVAL),
  // so run it through cmd.exe /c instead of using shell:true (which would re-parse the whole
  // command line and reopen an injection surface even though our inputs are already validated).
  let child;
  try {
    child = process.platform === "win32"
      ? spawn("cmd.exe", ["/d", "/s", "/c", mvnExe, ...mavenArgs], { cwd: PROJECT_ROOT, windowsHide: false })
      : spawn(mvnExe, mavenArgs, { cwd: PROJECT_ROOT });
  } catch (e) {
    fs.rmSync(tempSuitePath, { force: true });
    return sendJson(res, 500, { error: `Failed to launch Maven (${mvnExe}): ${e.message}` });
  }
  let output = "";
  const appendOutput = chunk => {
    output += chunk.toString();
    if (output.length > 200_000) output = output.slice(-200_000); // cap captured log size
  };
  child.stdout.on("data", appendOutput);
  child.stderr.on("data", appendOutput);

  const killTimer = setTimeout(() => child.kill(), RUN_TIMEOUT_MS);
  child.on("close", code => {
    clearTimeout(killTimer);
    fs.rmSync(tempSuitePath, { force: true });
    sendJson(res, 200, { exitCode: code, success: code === 0, output: output.slice(-20_000) });
  });
  child.on("error", e => {
    clearTimeout(killTimer);
    fs.rmSync(tempSuitePath, { force: true });
    sendJson(res, 500, { error: `Failed to launch Maven (${mvnExe}): ${e.message}` });
  });
}

function handleApi(req, res, parsed) {
  const { pathname, query } = parsed;

  if (req.method === "GET" && pathname === "/api/pages") {
    return sendJson(res, 200, listJavaFiles(PAGES_DIR));
  }
  if (req.method === "GET" && pathname === "/api/tests") {
    return sendJson(res, 200, listJavaFiles(TESTS_DIR));
  }

  if (req.method === "GET" && pathname === "/api/source") {
    const safePath = resolveSafeJavaPath(query.file);
    if (!safePath) return sendJson(res, 400, { error: "Invalid or disallowed file path" });
    if (!fs.existsSync(safePath)) return sendJson(res, 404, { error: "File not found" });
    const content = fs.readFileSync(safePath, "utf8");
    return sendJson(res, 200, { file: query.file, content });
  }

  if (req.method === "POST" && pathname === "/api/source") {
    return readBody(req, (err, body) => {
      if (err) return sendJson(res, 400, { error: err.message });
      const safePath = resolveSafeJavaPath(body.file);
      if (!safePath) return sendJson(res, 400, { error: "Invalid or disallowed file path" });
      if (typeof body.content !== "string") return sendJson(res, 400, { error: "content must be a string" });
      fs.mkdirSync(path.dirname(safePath), { recursive: true });
      fs.writeFileSync(safePath, body.content, "utf8");
      return sendJson(res, 200, { file: body.file, saved: true });
    });
  }

  if (req.method === "POST" && pathname === "/api/pages/new") {
    return readBody(req, (err, body) => {
      if (err) return sendJson(res, 400, { error: err.message });
      const className = (body.className || "").trim();
      if (!/^[A-Z][A-Za-z0-9]*$/.test(className)) return sendJson(res, 400, { error: "className must be a valid Java class name, e.g. CheckoutPage" });
      const abs = path.join(PAGES_DIR, `${className}.java`);
      const relativePath = path.relative(PROJECT_ROOT, abs).replace(/\\/g, "/");
      if (fs.existsSync(abs)) return sendJson(res, 409, { error: `${className}.java already exists` });
      fs.mkdirSync(PAGES_DIR, { recursive: true });
      const content = pageBoilerplate(className);
      fs.writeFileSync(abs, content, "utf8");
      return sendJson(res, 201, { file: relativePath, content, className, fqcn: `com.automation.pages.${className}` });
    });
  }

  if (req.method === "POST" && pathname === "/api/tests/new") {
    return readBody(req, (err, body) => {
      if (err) return sendJson(res, 400, { error: err.message });
      const className = (body.className || "").trim();
      if (!/^[A-Z][A-Za-z0-9]*$/.test(className)) return sendJson(res, 400, { error: "className must be a valid Java class name, e.g. CheckoutTest" });
      const abs = path.join(TESTS_DIR, `${className}.java`);
      const relativePath = path.relative(PROJECT_ROOT, abs).replace(/\\/g, "/");
      if (fs.existsSync(abs)) return sendJson(res, 409, { error: `${className}.java already exists` });
      fs.mkdirSync(TESTS_DIR, { recursive: true });
      const content = testBoilerplate(className);
      fs.writeFileSync(abs, content, "utf8");
      return sendJson(res, 201, { file: relativePath, content, className, fqcn: `com.automation.tests.${className}` });
    });
  }

  if (req.method === "GET" && pathname === "/api/execution-data") {
    if (!fs.existsSync(EXECUTION_DATA_FILE)) return sendJson(res, 200, {});
    const raw = fs.readFileSync(EXECUTION_DATA_FILE, "utf8");
    const match = raw.match(/const\s+EXECUTION_RESULTS\s*=\s*(\{[\s\S]*\})\s*;?\s*$/);
    if (!match) return sendJson(res, 200, {});
    try {
      return sendJson(res, 200, JSON.parse(match[1]));
    } catch (e) {
      return sendJson(res, 200, {});
    }
  }

  if (req.method === "POST" && pathname === "/api/run-test") {
    return readBody(req, (err, body) => {
      if (err) return sendJson(res, 400, { error: err.message });
      const automationKey = (body.automationKey || "").trim();
      if (!AUTOMATION_KEY_PATTERN.test(automationKey)) {
        return sendJson(res, 400, { error: "automationKey must look like com.automation.tests.LoginTest#methodName" });
      }
      const browser = ALLOWED_BROWSERS.has(body.browser) ? body.browser : "edge";
      const headless = body.headless === true; // default to headed so you can watch it run
      // Plain `-Dtest=Class#method` makes Surefire build its own dynamic suite and ignore the
      // project's testng.xml <listeners> block, so TestCaseTracker/execution-data.js never gets
      // updated. A one-off suite file (with the same listener) keeps that integration working.
      runMavenSuite(res, buildSuiteXml([automationKey]), browser, headless);
    });
  }

  if (req.method === "POST" && pathname === "/api/run-all") {
    return readBody(req, (err, body) => {
      if (err) return sendJson(res, 400, { error: err.message });
      const automationKeys = Array.isArray(body.automationKeys) ? body.automationKeys.map(k => (k || "").trim()) : [];
      if (automationKeys.length === 0) return sendJson(res, 400, { error: "automationKeys must be a non-empty array" });
      const invalid = automationKeys.find(k => !AUTOMATION_KEY_PATTERN.test(k));
      if (invalid) return sendJson(res, 400, { error: `Invalid automationKey: ${invalid}` });
      const browser = ALLOWED_BROWSERS.has(body.browser) ? body.browser : "edge";
      const headless = body.headless === true;
      runMavenSuite(res, buildSuiteXml(automationKeys), browser, headless);
    });
  }

  return sendJson(res, 404, { error: "Not found" });
}

function serveStatic(req, res, pathname) {
  let relPath = pathname === "/" ? "/index.html" : pathname;
  const safe = path.normalize(relPath).replace(/^(\.\.[/\\])+/, "");
  const filePath = path.join(TRACKER_ROOT, safe);
  if (!filePath.startsWith(TRACKER_ROOT)) {
    res.writeHead(403);
    return res.end("Forbidden");
  }
  fs.readFile(filePath, (err, data) => {
    if (err) {
      res.writeHead(404, { "Content-Type": "text/plain" });
      return res.end("Not found");
    }
    const ext = path.extname(filePath);
    res.writeHead(200, { "Content-Type": MIME[ext] || "application/octet-stream" });
    res.end(data);
  });
}

const server = http.createServer((req, res) => {
  const parsed = url.parse(req.url, true);
  if (parsed.pathname.startsWith("/api/")) {
    return handleApi(req, res, parsed);
  }
  return serveStatic(req, res, parsed.pathname);
});

server.listen(PORT, HOST, () => {
  console.log(`Test Case Tracker running at http://localhost:${PORT} (API enabled for editing Page/Test source files)`);
});

// This is a local dev tool, not a production server - keep it alive rather than let one bad
// request or child-process error take down the whole tracker.
process.on("uncaughtException", err => console.error("Unexpected error (server kept running):", err));
