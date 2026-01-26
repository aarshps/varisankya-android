---
name: Project Cleanup Guidelines
description: Guidelines for keeping the repository clean of temporary and build files
---

# Project Cleanup Guidelines

This skill ensures the repository stays clean of temporary files.

## Never Create in Repository Root

Do NOT create these file types in the repository:
- `build_log*.txt` - Build output logs
- `*.log` - Log files
- `*.hprof` - Heap dumps
- `output*.txt` - Command output captures
- `temp*.txt` / `tmp*.txt` - Temporary files

## If Build Logs Are Needed

1. **Use the terminal output directly** - Gradle commands output to console
2. **Use `gradlew --info` or `--debug`** - For verbose output
3. **If a file is absolutely needed**, create it in system temp:
   ```powershell
   $env:TEMP\build_output.txt
   ```

## Before Ending a Session

Run cleanup check:
```powershell
# Check for stray files
Get-ChildItem -Path . -File | Where-Object { $_.Name -match "(build_log|output|temp|tmp|detailed_build)" }

# Remove if found
Remove-Item -Path "build_log*.txt" -Force -ErrorAction SilentlyContinue
Remove-Item -Path "detailed_build_log.txt" -Force -ErrorAction SilentlyContinue
Remove-Item -Path "*.log" -Force -ErrorAction SilentlyContinue
Remove-Item -Path "*.txt" -Force -ErrorAction SilentlyContinue
Remove-Item -Path "build_final*.txt" -Force -ErrorAction SilentlyContinue
```

## .gitignore Already Excludes

The `.gitignore` should already exclude:
- `/build/`
- `/.gradle/`
- `/.idea/`
- `*.log`
- `*.hprof`

### User-Generated Temporary Files
- `*.log` (Build and compilation logs)
- `*.txt` (Temporary execution captures)
- `build_final*.txt` (Final validation logs)
 
### Android Specific
- `app/build/`
- `.gradle/`
- `local.properties` (unless required for CI)
- `*.hprof`

## Best Practice

Capture build output in memory or terminal, not files. Use tools that return output directly rather than redirecting to files in the repo.
```
