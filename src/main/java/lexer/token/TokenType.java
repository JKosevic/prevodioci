package lexer.token;

    public enum TokenType {

        // --- Tipovi podataka ---
        BROJ_ELIXIRA,      // brojElixira (int)
        SLOVO_KARTICE,     // char
        DOUBLE_ELIXIR,     // double
        IME_KARTICE,       // string
        IDENT,             // identifikator (ime promenljive ili funkcije)

        // --- Literali ---
        INT_LIT,           // 10, -5, +3, 0
        DOUBLE_LIT,        // 12.50, 3.00 ...
        STRING_LIT,        // "tekst"
        CHAR_LIT,          // 'A'
        HEX_LIT,           // HB4C
        OCT_LIT,           // O43

        // --- Ključne reči ---
        BATTLE,            // battle()
        KRAJ_BORBE,        // krajBorbe;
        UCITAJ_KARTICU,    // ucitajKarticu
        ISPISI_KARTICU,    // ispisiKarticu

        // Uslovne naredbe
        LEADER,            // leader (if)
        ELDER,             // elder  (else if)
        MEMBER,            // member (else)

        // For-petlja (cycle)
        CYCLE,             // cycle()

        // --- Definicije funkcija ---
        AT_TYPE,           // @brojElixira@
        BEZ_ELIXIRA,       // bezElixira (void)

        // --- Blokovi ---
        BLOCK_START,       // #
        BLOCK_END,         // $

        // --- Zagrade ---
        LPAREN, RPAREN,    // ( , )
        LBRACKET, RBRACKET,// [ , ]
        LBRACE_TERNARY,    // {  (za ternarni operator)
        RBRACE_TERNARY,    // }

        // --- Separatori ---
        COMMA,             // ,
        SEMICOLON,         // ;
        ASSIGN,            // =


        // --- Aritmetički operatori ---
        ADD,               // +
        SUB,               // -
        MULTIPLY,          // *
        DIVIDE,            // /
        PERCENT,           // %


        // --- Logički operatori ---
        LOG_AND,           // &
        LOG_OR,            // |
        LOG_NOT,           // !

        QUOTE,
        APOSTROPHE,

        // unarni operatori
        INCREMENT,      // ++
        DECREMENT,      // --

        // kombinovani operatori dodele
        PLUS_ASSIGN,    // +=
        MINUS_ASSIGN,
        MULT_ASSIGN,   // *=
        DIV_ASSIGN,

        // --- Relacioni operatori ---
        LT,                // <
        LE,                // <=
        GT,                // >
        GE,                // >=
        EQ,                // ==
        NEQ,               // !=

        // --- Ternarni operator ---
        TERNARY_QMARK,     // ?
        TERNARY_COLON,     // :

        // --- Specijalno ---
        NEWLINE,
        EOF,
        /// dodati ove stvari u lekser :


    }
