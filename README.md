# NAS Backup Tool

An automated backup solution that backs up files from your NAS to an external drive whenever the drive is connected to
your laptop.

## Features

- **Automatic Drive Detection**: Detects when external drives are connected
- **NAS Connectivity**: Connects to your NAS over SMB/CIFS protocol
- **Incremental Backup**: Only copies new or modified files
- **Progress Tracking**: Real-time progress display during backup
- **Backup History**: Maintains a log of all backup operations
- **Cross-Platform**: Works on Windows, Linux, and macOS

## Run

```bash
  mvn clean javafx:run
```

## TODO
- Refactor code & clean up into organized sections & use application.properties files for param
- Fix sections:
  - Detection of online drive (smb) & be able to select specific folder to copy 
   (works for selection of external drive ...)
  - Detection of external drive (not working)
  - Selection of destination copy
  - Disk space for external drive
  - Check of diskspace required for save
  - Historic
- Generate .exe file downloadable from git (not needed to have CI/CD to create artefact, just push)
- Versioning of application pushed .exe file
- Verify if script works