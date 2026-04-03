<table>
  <tr>
    <td>
      <img src="img/Icon_512x512_2_round.png" alt="App Logo" width="70">
    </td>
    <td>
      <h1>Wani (වනි) — Sinhala Dictation Keyboard</h1>
    </td>
  </tr>
</table>

**ඔබේ හඬ, ඔබේ භාෂාව** — *Your voice, your language*

A Sinhala-first AI dictation keyboard for Android, powered by OpenAI Whisper and Google Gemini.
Transcribe speech in Sinhala script, Singlish romanization, or directly to English — all from your keyboard.

---

## ⬇️ Download APK

**[→ Releases page](../../releases)** — Download the latest `wani-vX.X.X.apk` from the Releases tab above.

> The APK is also available as a **build artifact** from every CI run:
> go to the [Actions tab](../../actions), click the latest successful run, and download **`wani-debug-apk`**.

### Installation steps
1. Download the `.apk` file from the Releases page
2. On your Android device: **Settings → Apps → Special app access → Install unknown apps** → allow your browser
3. Tap the downloaded file to install
4. Go to **Settings → System → Languages & Input → On-screen keyboard → Manage keyboards** → enable **Wani**
5. Open any text field, tap the keyboard icon in the nav bar, and switch to **Wani**

---

## ✨ Features

| Feature | Description |
|---|---|
| 🇱🇰 **Sinhala language** | First-class Sinhala (සිංහල) support via Whisper & Gemini AI |
| 📝 **Output modes** | Sinhala script / Singlish romanization / English translation |
| 🤖 **Gemini AI** | Free-tier Google Gemini (15 req/min, 1M tokens/day) — no API cost |
| 🩺 **Profession modes** | General, Medical, Legal, IT, Custom — context-aware transcription |
| 📖 **Custom dictionary** | Add trigger words → auto-replacements applied after every transcription |
| 🎤 **Floating mic button** | Overlay mic button usable in any app |
| ⌨️ **Compact keyboard** | Single-row minimal keyboard mode |
| 🔄 **Multi-provider** | OpenAI · Groq · Google Gemini · Custom server |

---

## 🔑 API Keys

- **Google Gemini (recommended — free):** Get your key at [aistudio.google.com](https://aistudio.google.com/app/apikey)
- **OpenAI:** [platform.openai.com/api-keys](https://platform.openai.com/api-keys)
- **Groq (free tier):** [console.groq.com](https://console.groq.com)

---

## 📲 Screenshots

| Keyboard | Settings |
|---|---|
| ![keyboard](img/dictate_keyboard_notes.png) | ![settings](img/dictate_settings.png) |

---

## 🛠️ Build from source

```bash
git clone https://github.com/serdul/Dictate-Sinhala.git
cd Dictate-Sinhala
./gradlew assembleDebug
# APK output: app/build/outputs/apk/debug/app-debug.apk
```

Requires Android SDK 26+ and JDK 17.

---

## 📞 Contact

Made with ❤️ from Sri Lanka 🇱🇰

**Seran Senevirathna** · [WhatsApp +94712104933](https://wa.me/94712104933)

---

## License

Based on [Dictate](https://github.com/DevEmperor/Dictate) by DevEmperor.
This project is under the terms of the [Apache 2.0 license](https://www.apache.org/licenses/LICENSE-2.0).
