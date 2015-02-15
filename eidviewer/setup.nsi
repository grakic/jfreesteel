; Installer for "JFreesteel eID Viewer"

;======================================================
; Includes

  !include MUI.nsh
  !include Sections.nsh
  !include target\project.nsh

;======================================================
; Installer Information

  Name "${PROJECT_NAME}"

  SetCompressor /SOLID lzma
  XPStyle on
  CRCCheck on
  InstallDir "C:\Program Files\JFreesteel\${PROJECT_ARTIFACT_ID}\"
  AutoCloseWindow false

;======================================================
; Version Tab information for Setup.exe properties

  VIProductVersion 1.1.0.1
  VIAddVersionKey ProductName "${PROJECT_NAME}"
  VIAddVersionKey ProductVersion "${PROJECT_VERSION}"
  VIAddVersionKey CompanyName "${PROJECT_ORGANIZATION_NAME}"
  VIAddVersionKey FileVersion "${PROJECT_VERSION}"
  VIAddVersionKey FileDescription "${PROJECT_NAME}"
  VIAddVersionKey LegalCopyright ""

;======================================================
; Variables


;======================================================
; Modern Interface Configuration

  !define MUI_HEADERIMAGE
  !define MUI_ABORTWARNING
  !define MUI_COMPONENTSPAGE_SMALLDESC
  !define MUI_HEADERIMAGE_BITMAP_NOSTRETCH
  !define MUI_FINISHPAGE
  !define MUI_FINISHPAGE_TEXT "Thank you for installing ${PROJECT_NAME}."
  !define MUI_ICON "${NSISDIR}\Contrib\Graphics\Icons\orange-install.ico"

;======================================================
; Modern Interface Pages

  !define MUI_DIRECTORYPAGE_VERIFYONLEAVE
  !insertmacro MUI_PAGE_DIRECTORY
  !insertmacro MUI_PAGE_INSTFILES
  !insertmacro MUI_PAGE_FINISH

;======================================================
; Languages

  !insertmacro MUI_LANGUAGE "English"

;======================================================
; Installer Sections

Section "EidViewer"
    SetOutPath $INSTDIR
    SetOverwrite on
    File /r ${PROJECT_BUILD_DIR}/dependency
    File ${PROJECT_BUILD_DIR}/eidviewer.exe

    writeUninstaller "$INSTDIR\uninstall.exe"

    createShortCut "$SMPROGRAMS\JFreesteel\eID Viewer.lnk" "$INSTDIR\eidviewer.exe"
    createShortCut "$DESKTOP\${PROJECT_NAME}.lnk" "$INSTDIR\eidviewer.exe"
SectionEnd


Section "uninstall"
  delete "$INSTDIR\eidviewer.exe"
  delete "$INSTDIR\uninstall.exe"
  RMDir /r "$INSTDIR\dependencies"
SectionEnd

Function .onInit
    InitPluginsDir
FunctionEnd
