# PINS-Compiler
"Compiler" or. rather an interpreter for PINS programming language

## Usage
To compile your PINS code using the PINS Compiler, please follow these steps:
1. Clone repository from GitHub.
2. `cd` into `out\artifacts\PINSCompiler_jar` folder.
3. To compile your PINS code, run the following command:
  ```
   java -jar .\PINSCompiler.jar PINS <path_to_source_file> [flags]
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
  
## Example programs
### Standard Library
The PINS programming language provides a standard library that includes the following functions:
1. `print_int(value: integer)` : This function is used to print integer values to the console.
2. `print_str(value: string)` : This function is used to print string values to the console.
3. `print_log(value: logical)` : This function is used to print logical (boolean) values to the console.
4. `rand_int(min: integer, max: integer)` : This function generates a random integer value between the specified minimum and maximum values.
5. `seed(value: integer)` : This function sets the seed for the random number generator used by the `rand_int` function.

### Examples of Valid Programs:
Fibonacci numbers:
```
fun fib(n:integer) : integer = (
    {
        if n <= 1 then
            { result = n }
        else
            { result = fib(n-1) + fib(n-2) }
    },
    result
) { where var result : integer };

fun main(arg : integer) : integer = (
    {for i=1, 10, 1: (

        print_int(fib(i))

    )} {where var i : integer},
    0
)
```
Output:
```
1
1
2
3
5
8
13
21
34
```

While loop:
```
typ int: integer;

fun main(x: integer): integer = (
    { i = 0 },
    { while i < 15: (
        print_int(i),
        { i = increment(i) }
    )},
    i
) { where
    var i: int;
    fun increment(x: integer): integer = (
        { x = x + 1 },
        x
    )
}
```
Output:
```
0
1
2
3
4
5
6
7
8
9
10
11
12
13
14
```
  
For loop:
```
  fun main(x: integer): integer = (
    { for x = 1, 130, x * 2: (
        print_int(x)
    )},
    0
  )
```
Output:
```
1
3
9
27
81
```
  
If statement:
```
fun main(x: integer): integer = (
    { if larger_than_10(5) then
        print_str('5 is larger than 10')
      else
        print_str('5 is not larger than 10')
    },
    { if larger_than_10(20) then
        print_str('20 is larger than 10')
      else
        print_str('20 is not larger than 10')
    },
    print_log(f(5, 10)),
    print_log(f(10, 5)),
    0
) { where
    typ boolean: logical;
    fun f(x1: integer, x2: integer): boolean = (
        { if x1 < x2 then
            print_str('x1 < x2')
        else
            print_str('x1 >= x2')
        },
        true
    );
    fun larger_than_10(x1: integer): boolean = (
        { larger = false },
        { if x1 > 10 then
            { larger = true }
        },
        larger
    ) { where
        var larger: logical
    }
}
```
Output:
```
"5 is not larger than 10"
"20 is larger than 10"
"x1 < x2"
true
"x1 >= x2"
true
```
