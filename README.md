<div align="center">
  <img src="https://github.com/Urban-Elf/InvisionArchiveTools/blob/main/app/src/main/resources/icon.svg" alt="Invision Archive Tools icon">

<h3>Invision Archive Tools</h3>
v1.0.2

  <p>&nbsp;</p>

  <div>
    <img src="https://img.shields.io/github/v/release/Urban-Elf/InvisionArchiveTools?label=version&color=violet" alt="Version">
    <img src="https://img.shields.io/badge/build-passing-brightgreen" alt="Build: Passing">
    <img src="https://img.shields.io/badge/license-GPLv3-blue" alt="License: GPLv3">
  </div>

  <div>
    <img src="https://img.shields.io/badge/Java-17+-orange?logo=java&logoColor=white" alt="Java">
    <img src="https://img.shields.io/badge/Python-3.11+-blue?logo=python&logoColor=white" alt="Python">
    <img src="https://img.shields.io/badge/HTML-5-red?logo=html5&logoColor=white" alt="HTML5">
    <img src="https://img.shields.io/badge/CSS-3-blue?logo=css3&logoColor=white" alt="CSS3">
    <img src="https://img.shields.io/badge/JavaScript-ES6+-yellow?logo=javascript&logoColor=black" alt="JavaScript">
  </div>
</div>

---

## Overview

Invision Archive Tools (IAT) is a user-friendly tool that enables members of Invision-based communities to convert a variety of different types of content into offline copies.

Current content model:

| Content Type   | Supported  |
|----------------|------------|
| Messengers     | ✅          |
| Topics         | ❌          |
| Blog entries   | ❌          |
| Gallery images | ❌          |

> ⚠️ **Please note:** This tool is in early development, so support for further content types will emerge gradually as the project progresses. Thank you for your patience!

---

## Installation

Native OS installers, as well as a standalone JAR, are available on the [releases page](https://github.com/Urban-Elf/InvisionArchiveTools/releases). It should be noted that if you are going to use the JAR, you will also need a Java Runtime Environment (JRE) installed for Java 17+.

The tool itself requires the latest version of Chrome to be installed as well, which can be downloaded [here](https://google.com/chrome).

---

## Usage

Upon startup, you will be prompted to read and agree to the [Terms of Service](TOS.md). From there, you will be directed to the main window, shown below.

![](https://github.com/Urban-Elf/InvisionArchiveTools/blob/main/docs/00.png)

From there, you will need to specify an Invision community. In the right panel, type the domain (e.g. catholicharbor.com) into the field, and press the <kbd>+</kbd> button in the UI or <kbd>Enter</kbd> on the keyboard to add it.

You will also need to set it as the active community. Simply select it from the dropdown on the bottom right.

![](https://github.com/Urban-Elf/InvisionArchiveTools/blob/main/docs/01.png)
![](https://github.com/Urban-Elf/InvisionArchiveTools/blob/main/docs/01-1.png)

Now you are ready to begin archiving! Choose a content type from the list on the left. In this example, we'll be converting a messenger.

![](https://github.com/Urban-Elf/InvisionArchiveTools/blob/main/docs/03.png)

After a short while initializing, an independent, automated session of Chrome will launch. You will then be prompted to sign in to your account on the community.

> ⚠️ **Disclaimer:** This tool does NOT collect, process, or transmit login credentials, authentication tokens, or any personal account information. All such operations are performed between the client and browser with standard privacy procedures, independent and undetected by the App.

![](https://github.com/Urban-Elf/InvisionArchiveTools/blob/main/docs/04.png)

Following this, simply adhere to the instructions as they come in the pop-up window of the app, and wait for the archiving process to complete. It is important that you avoid interrupting the Chrome browser session during this time (such as following links, opening tabs, switching tabs, etc.), as the automation software is not designed for this, and you will need to restart the task.

Once completed, you will be able to export the archive in a format of your choosing. There are currently two formats supported, listed below.

| Format                      | Usage                                                                                                                                        |
|-----------------------------|----------------------------------------------------------------------------------------------------------------------------------------------|
| HTML (default, recommended) | Viewing your content as you would normally on the Invision community. This is likely the most relevant format to you as a user of this tool. |
| JSON                        | Data processing and manipulation, which is beyond the scope of this short usage guide.                                                       |

The folder structure of the resulting archive will appear similar to the image below. **The folders titled `page` and `res` contain supporting resources, and are required for proper function.**

![](https://github.com/Urban-Elf/InvisionArchiveTools/blob/main/docs/05.png?raw=true)

To view it, open the file with the `.html` extension in a web browser of preference, and enjoy!

![](https://github.com/Urban-Elf/InvisionArchiveTools/blob/main/docs/06.png)

---

## How to Contribute

Anybody can contribute to this tool by playing around with it, [reporting bugs](https://github.com/Urban-Elf/InvisionArchiveTools/issues/new?template=bug-report.yml), or [suggesting new features](https://github.com/Urban-Elf/InvisionArchiveTools/issues/new?template=feature.yml)!

If you do, however, possess knowledge and experience with any of the languages used in the software, you can contact the developer via email at iat.legacy037@aleeas.com, and you can talk with him about contributions to the codebase.

**Currently seeking MacOS testers and web developers.**

Thank you for your support!

## License

[GNU General Public License v3](LICENSE)
