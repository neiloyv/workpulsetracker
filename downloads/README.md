# Place built agent installers here (not committed):
#   workpulsetracker-agent-windows.zip   (recommended, no WiX)
#   workpulsetracker-agent-windows.msi   (needs WiX Toolset)
#   workpulsetracker-agent-macos.dmg
#   workpulsetracker-agent-linux.deb
#
# Windows portable ZIP (no WiX):
#   .\gradlew :tracker-agent:publishWindowsDownload
#
# Windows MSI (requires WiX light.exe/candle.exe on PATH):
#   .\gradlew :tracker-agent:publishWindowsMsi
#
# Server serves them at GET /downloads/<fileName>
# Set DOWNLOAD_DIRECTORY if the server working directory is not the repo root.
