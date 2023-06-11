# PINS-Compiler
"Compiler" or. rather an interpreter for PINS programming language

## Installation
### Note: This installation process will only work on Windows operating system.
Before installing the PINS Compiler, please ensure that you have the following prerequisites:
- Java 19 or later installed on your system.

To install the PINS Compiler, please follow these steps:
1. Download the PINSCompiler.zip folder from the [official github repo](https://github.com/cadezd/PINS-Compiler/releases).
2. Unzip the downloaded package to extract the contents.
3. Locate the extracted folder named `PINSCompiler`.
4. Move the `PINSCompiler` folder into the `C:\Program Files\` folder.
5. Add path location to (including) `PINSCompiler` folder to enviroment variables.

### Note: It is important to place the "PINSCompiler" folder under the "Program Files" folder for the compiler to function correctly.
Congratulations! You have successfully installed the PINS Compiler on your Windows system.

## Usage
To compile your PINS code using the PINS Compiler, please follow these steps:
1. Open a command prompt window.
2. To compile your PINS code, run the following command:
  ```
  PINS <path_to_source_file> [flags]
  ```
  Replace `<path_to_source_file>` with the path to your PINS source file.
  You can also include optional flags to customize the compilation process:
  - `--dump`: Outputs the result of a specific phase of the compiler. Valid values for `<phase>` are `LEX` (lexical analysis), `SYN` (syntax analysis), `AST` (abstract syntax), `NAME` (name checker), `TYP` (type checker),     `FRM` (frames), `IMC` (intermediate code), and `INT` (interpreter). For example:
    ```
    PINS <path_to_source_file> --dump <phase>
    ```
  - `--exec`: Specifies the phase of the compiler that will be executed last. Valid values for <phase> are `LEX`, `SYN`, `AST`, `NAME`, `TYP`, `FRM`, `IMC`, and `INT`. For example:
    ```
    PINS <path_to_source_file> --exec <phase>
    ```
  - `--memory`: Sets the memory size for interpreter. For example:
    ```
    PINS <path_to_source_file> --memory <size>
    ```
    Replace `<size>` with the desired memory size in bytes.
3. The compiler will process your PINS code and generate the corresponding output.
