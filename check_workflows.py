import urllib.request
import json
import os

repo = "Dynag1/Dynag1-Fdroid"
url = f"https://api.github.com/repos/{repo}/actions/runs?per_page=5"

try:
    response = urllib.request.urlopen(url)
    data = json.loads(response.read().decode())
    runs = data.get("workflow_runs", [])
    if not runs:
        print("No workflow runs found.")
    else:
        for run in runs:
            print(f"Run ID: {run['id']}")
            print(f"Name: {run['name']}")
            print(f"Status: {run['status']}")
            print(f"Conclusion: {run['conclusion']}")
            print(f"Branch/Tag: {run['head_branch']}")
            print(f"Event: {run['event']}")
            print("---")
except Exception as e:
    print(f"Error querying GitHub API: {e}")
