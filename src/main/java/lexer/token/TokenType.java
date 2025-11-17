package lexer.token;

    public enum TokenType {


        BROJ_ELIXIRA,
        SLOVO_KARTICE,
        DOUBLE_ELIXIR,
        IME_KARTICE,
        IDENT,


        INT_LIT,
        DOUBLE_LIT,
        STRING_LIT,
        CHAR_LIT,
        HEX_LIT,
        OCT_LIT,


        BATTLE,
        KRAJ_BORBE,
        UCITAJ_KARTICU,
        ISPISI_KARTICU,


        LEADER,
        ELDER,
        MEMBER,


        CYCLE,


        AT_TYPE,
        BEZ_ELIXIRA,


        BLOCK_START,
        BLOCK_END,


        LPAREN, RPAREN,
        LBRACKET, RBRACKET,
        LBRACE_TERNARY,
        RBRACE_TERNARY,


        COMMA,
        SEMICOLON,
        ASSIGN,



        ADD,
        SUB,
        MULTIPLY,
        DIVIDE,
        PERCENT,



        LOG_AND,
        LOG_OR,
        LOG_NOT,

        QUOTE,
        APOSTROPHE,


        INCREMENT,
        DECREMENT,


        PLUS_ASSIGN,
        MINUS_ASSIGN,
        MULT_ASSIGN,
        DIV_ASSIGN,


        LT,
        LE,
        GT,
        GE,
        EQ,
        NEQ,


        TERNARY_QMARK,
        TERNARY_COLON,


        NEWLINE,
        EOF,



    }
