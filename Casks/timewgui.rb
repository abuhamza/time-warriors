cask "timewgui" do
  version "1.1.0"
  sha256 "26158ccbd6c3132a25d69d20c0f36bab3f8d269dccc64fb97d44e24b4a7e5134"

  url "https://github.com/abuhamza/time-warriors/releases/download/v#{version}/TimewGUI-#{version}.dmg"
  name "TimewGUI"
  desc "Compose Multiplatform desktop GUI for Timewarrior"
  homepage "https://github.com/abuhamza/time-warriors"

  depends_on formula: "timewarrior"

  app "TimewGUI.app"
end
