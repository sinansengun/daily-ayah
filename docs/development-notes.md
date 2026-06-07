# Development Notes

## XcodeGen and iOS project path

- Generate the Xcode project under ios: `xcodegen generate -p ios -r ios`
- Keep iOS target paths in `project.yml` relative to ios root, for example `App/...` and `Widget/...`
- Build with: `xcodebuild -project ios/DailyAyah.xcodeproj ...`

## VS Code task editing tip

- When replacing text via Perl, `${workspaceFolder}` can be interpolated.
- To write it literally, escape the dollar sign: `\${workspaceFolder}`
