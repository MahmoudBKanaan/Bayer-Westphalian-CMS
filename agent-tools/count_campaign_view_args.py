from pathlib import Path


def count_args(body: str) -> int:
    args = []
    depth = 0
    cur = []
    in_s = None
    i = 0
    while i < len(body):
        c = body[i]
        if in_s:
            cur.append(c)
            if c == "\\" and i + 1 < len(body):
                cur.append(body[i + 1])
                i += 2
                continue
            if c == in_s:
                in_s = None
            i += 1
            continue
        if c in "\"'":
            in_s = c
            cur.append(c)
            i += 1
            continue
        if c == "(":
            depth += 1
            cur.append(c)
            i += 1
            continue
        if c == ")":
            depth -= 1
            cur.append(c)
            i += 1
            continue
        if c == "," and depth == 0:
            args.append("".join(cur).strip())
            cur = []
            i += 1
            continue
        cur.append(c)
        i += 1
    if "".join(cur).strip():
        args.append("".join(cur).strip())
    return len(args)


def extract_constructors(text: str):
    results = []
    needle = "new CampaignView("
    start = 0
    while True:
        i = text.find(needle, start)
        if i < 0:
            break
        j = i + len(needle)
        depth = 1
        k = j
        while k < len(text) and depth:
            if text[k] == "(":
                depth += 1
            elif text[k] == ")":
                depth -= 1
            k += 1
        body = text[j : k - 1]
        line = text[:i].count("\n") + 1
        results.append((line, count_args(body)))
        start = k
    return results


files = [
    Path(
        r"backend/src/test/java/com/bayerwestphalian/campaign/campaign/CampaignControllerTests.java"
    ),
    Path(
        r"backend/src/test/java/com/bayerwestphalian/campaign/campaign/CampaignDetailsCanBeLoadedTests.java"
    ),
    Path(
        r"backend/src/test/java/com/bayerwestphalian/campaign/campaign/CampaignRejectionReasonTests.java"
    ),
    Path(
        r"backend/src/test/java/com/bayerwestphalian/campaign/campaign/CampaignCanBeRejectedTests.java"
    ),
]

for path in files:
    text = path.read_text(encoding="utf-8")
    for line, n in extract_constructors(text):
        mark = "OK" if n == 21 else "BAD"
        print(f"{path.name}:{line}: {n} args [{mark}]")
