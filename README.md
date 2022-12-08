# SimplePortCommunicator

> Copyright 2022, Matthijs Veldkamp

Simple TUI program to communicate with a socket in plain text.

You can connect to a server, knowing its URL or IP address, and port. After this, you

## How to use
Either download the `.jar` file or clone the repository in order to use the program. If the application is started from a terminal that meets the minimum requirements of the TUI, the program will be started inside the terminal itself. If the terminal does not meet the minimal requirements, a separate window will be created instead, in which the application will run. If you run the application by double clicking the `.jar` file, no terminal will be available, which will also result in a separate terminal window appearing.

The separate-window terminal generally works a bit better than others, and is also the terminal with which the application was tested. If you run into issues running the program in your own terminal, try running it

### JAR-file
You can run the `.jar` file by typing `java -jar SimplePortCommunicator.jar` in a terminal, or (generally) double clicking the file. It should be run with Java SE 11 or a newer version.

### From source
You can clone this repository and run the application yourself. The repository uses Maven to manage dependencies, and should be runnable through it too. The `main` method is located in `src/main/kotlin/Main.kt`.

## Features
- Connect to a server using its IP/URL and port.
- Send plain text messages to the server, once it's connected.
- Print the received responses from the server to the screen of the client.

### Planned features
- Set up automatic responses. (E.g: a server sending 'PING' will result in an immediate response 'PONG')
  - These will need to be set up by the user, as there is no way to know the server's protocol
- Allow the user to send non-plaintext data, such as binary data.

## Libraries
This application makes use of the [Lanterna text-based GUI library](https://github.com/mabe02/laterna). This library, just like this application, is licenced under the GNU Lesser General Public License, Version 3.
