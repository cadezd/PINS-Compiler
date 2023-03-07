package compiler.lexer;

public class CharStream {
    private String text;
    private int idx;
    private int line;
    private int column;
    private int prevColumn;

    public CharStream(String text) {
        this.text = text;
        this.idx = 0;
        this.line = 1;
        this.column = 1;
        this.prevColumn = 0;
    }

    public char nextChar() {
        if (this.idx >= this.text.length()) // EOF
            return '\0';

        char c = this.text.charAt(this.idx);
        this.idx++;

        if (c == '\n') {
            this.line++;
            this.prevColumn = column;
            this.column = 1;
        } else if (c == '\t') { // TAB equals 4 spaces
            this.column += 4;
        } else
            this.column++;

        return c;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    public void back() {
        if (this.idx <= 0)
            return;

        this.idx--;

        char c = this.text.charAt(this.idx);

        if (c == '\n') {
            this.line--;
            this.column = this.prevColumn;
        } else if (c == '\t') { // TAB equals 4 spaces
            this.column -= 4;
        } else
            this.column--;
    }
}