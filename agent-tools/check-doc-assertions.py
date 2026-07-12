import re
from pathlib import Path

root = Path(r"D:\Documents\IU\AgileApp")
test_dir = root / "backend/src/test/java/com/bayerwestphalian/campaign/product"
failures = []

for test_file in sorted(test_dir.glob("*DocumentationTests.java")):
    text = test_file.read_text(encoding="utf-8")
    methods = re.split(r"(?=@Test)", text)
    for method in methods:
        if "Files.readString" not in method or ".contains(" not in method:
            continue
        path_match = re.search(r'Path\.of\("([^"]+)"\)', method)
        if not path_match:
            continue
        doc_rel = path_match.group(1)
        doc_path = (root / "backend" / doc_rel).resolve()
        content = doc_path.read_text(encoding="utf-8")
        for match in re.finditer(r'\.contains\(\s*"((?:\\.|[^"\\])*)"', method):
            val = bytes(match.group(1), "utf-8").decode("unicode_escape")
            if val not in content:
                failures.append((test_file.name, val, str(doc_path.relative_to(root))))
        for match in re.finditer(
            r'\.contains\(\s*\n\s*"([^"]*)"\s*\+\s*\n\s*"([^"]*)"', method
        ):
            val = match.group(1) + match.group(2)
            if val not in content:
                failures.append((test_file.name, val, str(doc_path.relative_to(root))))

print(f"FAILURES {len(failures)}")
for item in failures:
    print(item)