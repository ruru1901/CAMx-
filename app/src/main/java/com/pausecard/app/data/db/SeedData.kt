package com.pausecard.app.data.db

import com.pausecard.app.data.entity.CardEntity
import com.pausecard.app.data.entity.InterceptedAppEntity

object SeedData {

    private val seedCards = listOf(
        // ECE
        CardEntity(
            title = "Op-Amp as a Comparator",
            body = "An op-amp compares two voltages and outputs HIGH or LOW based on which input is larger. " +
                    "No feedback resistor means it runs open-loop with massive gain, snapping to rail voltages. " +
                    "Use it to detect threshold crossings — like a battery low-voltage alarm. " +
                    "Hook Vref to one input and your signal to the other; output flips when signal crosses Vref.",
            category = "ECE",
            isStatic = true
        ),
        CardEntity(
            title = "What a Decoupling Capacitor Actually Does",
            body = "Decoupling caps sit right next to IC power pins and act as tiny local energy reservoirs. " +
                    "When the chip draws a quick current spike, the cap supplies it before the power trace can. " +
                    "Without them, voltage dips cause glitches and digital circuits reset themselves. " +
                    "Use 100nF ceramic for high-frequency noise and 10uF bulk for lower-frequency ripple.",
            category = "ECE",
            isStatic = true
        ),
        CardEntity(
            title = "RC Time Constant in One Sentence",
            body = "Tau equals R times C, and it tells you how fast a capacitor charges or discharges through a resistor. " +
                    "After one tau, the capacitor reaches about 63% of its final voltage. " +
                    "After five taus, it's considered fully charged at over 99%. " +
                    "Design a simple delay circuit by picking R and C so five tau equals your desired delay.",
            category = "ECE",
            isStatic = true
        ),
        CardEntity(
            title = "Pull-Up Resistors Explained Simply",
            body = "A pull-up resistor holds a digital input HIGH by default when nothing is driving it. " +
                    "When a button connects the line to ground, the input goes LOW — that's your signal. " +
                    "Without the pull-up, the pin floats and picks up random noise. " +
                    "Use 4.7kΩ to 10kΩ for most 3.3V and 5V logic; lower values waste current, higher values are slow.",
            category = "ECE",
            isStatic = true
        ),
        CardEntity(
            title = "Bode Plot in 30 Seconds",
            body = "A Bode plot shows how a circuit's gain and phase change with frequency on a log scale. " +
                    "The magnitude plot uses dB so you can see multiplication as addition visually. " +
                    "Slopes tell you filter order: -20dB/decade per pole for a low-pass. " +
                    "Check where gain crosses 0dB — that's roughly your bandwidth.",
            category = "ECE",
            isStatic = true
        ),
        CardEntity(
            title = "Why PLLs Matter in Digital Design",
            body = "A Phase-Locked Loop takes a reference clock and generates a higher-frequency output clock. " +
                    "Your FPGA or MCU crystal might be 50MHz, but the core needs 500MHz — the PLL does that. " +
                    "It locks the output phase to the reference, so the multiplied clock stays stable. " +
                    "Most FPGAs have dedicated PLL primitives; just instantiate one in your schematic or HDL.",
            category = "ECE",
            isStatic = true
        ),
        CardEntity(
            title = "Thevenin Equivalent Makes Circuits Simple",
            body = "Any linear circuit with voltage sources and resistors looks like one voltage source and one series resistor. " +
                    "Find Vth by measuring open-circuit voltage. Find Rth by turning off sources and measuring resistance. " +
                    "This lets you analyze complex networks as a simple voltage divider. " +
                    "Use it when you need to figure out what load a circuit can actually drive.",
            category = "ECE",
            isStatic = true
        ),
        CardEntity(
            title = "What an ADC Resolution Bits Really Mean",
            body = "A 12-bit ADC divides its reference voltage into 4096 levels, so each step is Vref/4096. " +
                    "For a 3.3V reference, one LSB equals about 0.8mV — anything smaller gets lost. " +
                    "More bits means finer resolution but slower conversion and more noise susceptibility. " +
                    "Match your ADC bits to the precision you actually need; 12-bit is usually enough for sensors.",
            category = "ECE",
            isStatic = true
        ),
        CardEntity(
            title = "Impedance Matching Prevents Signal Reflection",
            body = "When a signal line's impedance doesn't match the load, part of the signal bounces back. " +
                    "At high frequencies this causes ringing, data errors, and EMI. " +
                    "Match the trace impedance to the load impedance using series or parallel resistors. " +
                    "USB, HDMI, and RF all require controlled impedance — usually 50Ω or 90Ω differential.",
            category = "ECE",
            isStatic = true
        ),
        CardEntity(
            title = "FIR vs IIR Filters — When to Use Which",
            body = "FIR filters use only past inputs and always produce a stable, linear-phase response. " +
                    "IIR filters use feedback, so they're more efficient but can become unstable. " +
                    "Use FIR when phase accuracy matters, like audio processing or communications. " +
                    "Use IIR when you need sharp cutoff with fewer coefficients and stability isn't a concern.",
            category = "ECE",
            isStatic = true
        ),

        // Cybersecurity
        CardEntity(
            title = "Buffer Overflow in Plain English",
            body = "A buffer overflow happens when a program writes more data into a memory buffer than it can hold. " +
                    "The extra data spills into adjacent memory, potentially overwriting return addresses or function pointers. " +
                    "An attacker can craft input that overwrites the return address with shellcode. " +
                    "Modern defenses include ASLR, stack canaries, and non-executable stacks — always enable them.",
            category = "Cybersecurity",
            isStatic = true
        ),
        CardEntity(
            title = "Why TLS Handshake Takes Two Round Trips",
            body = "In TLS 1.2, the client sends a ClientHello, the server responds with a certificate, " +
                    "then the client sends a key exchange — that's two round trips before data flows. " +
                    "TLS 1.3 cuts this to one round trip by letting the client send key share guesses upfront. " +
                    "0-RTT resumption sends data immediately on reconnect but is vulnerable to replay attacks.",
            category = "Cybersecurity",
            isStatic = true
        ),
        CardEntity(
            title = "SQL Injection Is Still Everywhere",
            body = "SQL injection happens when user input gets concatenated directly into a database query. " +
                    "The classic example: typing ' OR 1=1 -- in a login form makes the WHERE clause always true. " +
                    "Prepared statements with parameterized queries make this impossible — the DB treats input as data, not code. " +
                    "Never build SQL strings from user input, no matter how 'trusted' the source seems.",
            category = "Cybersecurity",
            isStatic = true
        ),
        CardEntity(
            title = "How Password Hashing Actually Works",
            body = "Plain SHA-256 of a password is fast — attackers can try billions per second with GPUs. " +
                    "bcrypt and Argon2 are deliberately slow and memory-hard, making brute-force expensive. " +
                    "bcrypt auto-salts and lets you tune the cost factor; Argon2 also resists GPU and side-channel attacks. " +
                    "Always hash passwords server-side, never in JavaScript — client hashing just adds a fake layer.",
            category = "Cybersecurity",
            isStatic = true
        ),
        CardEntity(
            title = "XSS: The Web's Persistent Headache",
            body = "Cross-site scripting injects malicious JavaScript into pages that other users then see. " +
                    "Stored XSS lives in the database (forum posts, comments); reflected XSS comes from URL parameters. " +
                    "Sanitize all output with context-aware encoding, not just string replacement. " +
                    "Content Security Policy headers give you a second line of defense by blocking inline scripts.",
            category = "Cybersecurity",
            isStatic = true
        ),
        CardEntity(
            title = "Principle of Least Privilege, Always",
            body = "Give every user, service, and process only the minimum permissions they need to do their job. " +
                    "If a web server process gets compromised, it can only access what that specific user can. " +
                    "Database users should have table-level permissions, not full admin access. " +
                    "Review and revoke unused permissions quarterly — they're a ticking time bomb if forgotten.",
            category = "Cybersecurity",
            isStatic = true
        ),
        CardEntity(
            title = "What a Firewall Actually Filters",
            body = "A traditional firewall filters packets based on source/destination IP and port numbers. " +
                    "It can block all incoming traffic on port 23 to stop telnet but can't see encrypted payloads. " +
                    "Next-gen firewalls do deep packet inspection and application-layer filtering. " +
                    "A firewall is one layer — defense in depth means combining it with other controls.",
            category = "Cybersecurity",
            isStatic = true
        ),
        CardEntity(
            title = "HMAC Proves Data Wasn't Tampered With",
            body = "HMAC combines a hash function with a secret key to create a message authentication code. " +
                    "Anyone with the key can verify the HMAC, but without it they can't forge one. " +
                    "Even if they know the hash algorithm, the secret key makes the output unpredictable. " +
                    "Use HMAC-SHA256 for API request signing, token verification, and file integrity checks.",
            category = "Cybersecurity",
            isStatic = true
        ),
        CardEntity(
            title = "Zero Trust: Never Trust, Always Verify",
            body = "Zero trust assumes every network, device, and user is potentially compromised. " +
                    "Instead of a trusted internal network, every request must prove its identity. " +
                    "Micro-segmentation limits lateral movement if one service gets breached. " +
                    "Implement with mTLS between services, short-lived tokens, and continuous verification.",
            category = "Cybersecurity",
            isStatic = true
        ),
        CardEntity(
            title = "JWT Tokens and Why They're Not Magic",
            body = "A JWT is three Base64-encoded parts: header, payload, and signature, separated by dots. " +
                    "The payload carries claims like user ID and expiry — but it's just encoded, not encrypted. " +
                    "Anyone can read the payload; the signature only prevents tampering if you verify it. " +
                    "Always validate the signature, check expiration, and keep tokens short-lived with refresh rotation.",
            category = "Cybersecurity",
            isStatic = true
        ),

        // Programming
        CardEntity(
            title = "Git Bisect Finds Bugs in Log Time",
            body = "git bisect does a binary search through your commit history to find which commit introduced a bug. " +
                    "Mark the current commit as bad and an old known-good commit as good. " +
                    "Git checks out the middle commit, you test it, mark it good or bad, and repeat. " +
                    "It finds the exact commit in log(n) steps instead of checking every single one.",
            category = "Programming",
            isStatic = true
        ),
        CardEntity(
            title = "Why Coroutines Beat Threads for I/O",
            body = "Threads are expensive — each one uses about 1MB of stack and the OS context-switches between them. " +
                    "Coroutines are lightweight functions that suspend instead of blocking, running on a small thread pool. " +
                    "You can launch thousands of coroutines where threads would choke. " +
                    "In Kotlin, use withContext(Dispatchers.IO) for blocking operations and keep the main thread free.",
            category = "Programming",
            isStatic = true
        ),
        CardEntity(
            title = "The Adapter Pattern Solves Interface Mismatch",
            body = "An adapter wraps an existing class with a different interface so it works where it's needed. " +
                    "Think of a power plug adapter: same electricity, different physical connector. " +
                    "In code, your new system expects a Charger interface but the old code outputs Volts — the adapter bridges them. " +
                    "Use it when you can't modify the existing class but need it to fit a new contract.",
            category = "Programming",
            isStatic = true
        ),
        CardEntity(
            title = "Bash One-Liner That Saves Hours",
            body = "find . -name '*.kt' -exec grep -l 'TODO' {} \\; lists every Kotlin file containing TODO. " +
                    "Combine with wc -l to count how many files have outstanding work. " +
                    "Pipe through xargs for bulk operations like: find . -name '*.bak' -delete. " +
                    "Master find, grep, xargs, and sed — you'll automate half your repetitive tasks.",
            category = "Programming",
            isStatic = true
        ),
        CardEntity(
            title = "HTTP Status Codes You Actually Need",
            body = "200 means success, 201 means created, 204 means success but no body to return. " +
                    "301 is permanent redirect, 302 is temporary, 304 means the client cache is still valid. " +
                    "400 is bad request, 401 is not authenticated, 403 is not authorized, 404 is not found. " +
                    "429 means rate-limited, 500 is server error, 503 means the server is overloaded or down.",
            category = "Programming",
            isStatic = true
        ),
        CardEntity(
            title = "REST vs GraphQL vs gRPC — Quick Picks",
            body = "REST is simple, cacheable, and every tool supports it — use it for most public APIs. " +
                    "GraphQL lets clients ask for exactly what they need, eliminating over-fetching on mobile. " +
                    "gRPC uses Protocol Buffers and HTTP/2 for fast, typed, bidirectional streaming between services. " +
                    "Pick REST for simplicity, GraphQL for complex client needs, gRPC for internal microservices.",
            category = "Programming",
            isStatic = true
        ),
        CardEntity(
            title = "What 'Clean Code' Actually Gets Wrong",
            body = "Over-abstracting simple logic makes code harder to understand, not easier. " +
                    "A 5-line function with a clear name beats a 20-line abstraction with four indirections. " +
                    "Comments explaining WHY are more valuable than comments explaining WHAT. " +
                    "Clean code means code you can understand in 30 seconds when you come back to it in 6 months.",
            category = "Programming",
            isStatic = true
        ),
        CardEntity(
            title = "Docker in 30 Seconds",
            body = "Docker containers package your app with its dependencies so it runs the same everywhere. " +
                    "A Dockerfile is a recipe: pick a base image, copy your code, install dependencies, set the command. " +
                    "docker build creates an image, docker run starts a container from that image. " +
                    "Use docker-compose.yml to define multi-container apps — database, backend, frontend all together.",
            category = "Programming",
            isStatic = true
        ),
        CardEntity(
            title = "Unit Tests vs Integration Tests",
            body = "Unit tests check one function in isolation, mocking everything else — they're fast and precise. " +
                    "Integration tests check if multiple parts work together, like API endpoint to database. " +
                    "Unit tests catch logic bugs; integration tests catch wiring bugs. " +
                    "Aim for fast unit tests on every commit, integration tests on PRs, and end-to-end tests before release.",
            category = "Programming",
            isStatic = true
        ),
        CardEntity(
            title = "Why Your App Needs Idempotency",
            body = "An idempotent operation gives the same result whether you run it once or a hundred times. " +
                    "If a user taps 'Pay' twice due to a network glitch, you don't want to charge them twice. " +
                    "Use idempotency keys on payment and order APIs — the server ignores duplicate requests with the same key. " +
                    "HTTP methods GET, PUT, and DELETE are naturally idempotent; POST and PATCH are not.",
            category = "Programming",
            isStatic = true
        ),

        // Tamil/Culture
        CardEntity(
            title = "Thirukkural — Verse 121 on Leadership",
            body = "\"The king who excels in the quality of not yielding to anger rules the earth.\" " +
                    "Thiruvalluvar wrote this 2000 years ago, yet it describes modern emotional intelligence. " +
                    "Great leaders don't react impulsively — they respond with calculated calm. " +
                    "This verse appears under the chapter 'Not Yielding to Anger', one of the most practical sections.",
            category = "Tamil",
            isStatic = true
        ),
        CardEntity(
            title = "Why Sangam Literature Is Unique",
            body = "Sangam literature (300 BCE – 300 CE) is one of the oldest classical literary traditions in the world. " +
                    "It organized poetry by landscape: coastal, mountain, and agricultural — each with its own emotions. " +
                    "Akam poetry describes love; Puram describes valor and ethics. " +
                    "This landscape-based literary system is found nowhere else in world literature.",
            category = "Tamil",
            isStatic = true
        ),
        CardEntity(
            title = "Madurai — The Athens of the East",
            body = "Madurai has been a continuous center of learning and trade for over 2500 years. " +
                    "The Sangam academies — where poets gathered and judges chose the best work — were based here. " +
                    "The Meenakshi Amman Temple complex covers 14 acres and has 33,000 sculptures. " +
                    "Greek and Roman traders mentioned Madurai in their records as a major commercial hub.",
            category = "Tamil",
            isStatic = true
        ),
        CardEntity(
            title = "Kolam: Math in Rice Flour Art",
            body = "Kolam is the daily floor art drawn with rice flour at the entrance of Tamil homes. " +
                    "Most kolams are based on mathematical grid patterns — dots connected by continuous lines. " +
                    "The rice flour feeds ants and small insects, making it an act of daily compassion. " +
                    "Some kolam patterns map to Euler paths and Hamiltonian cycles from graph theory.",
            category = "Tamil",
            isStatic = true
        ),
        CardEntity(
            title = "Silappatikaram — World's First Novel?",
            body = "Written by Ilango Adigal around the 2nd century CE, Silappatikaram tells the story of Kannagi. " +
                    "It's a complete narrative with character arcs, moral complexity, and social commentary. " +
                    "Many scholars consider it one of the earliest novels in world literature. " +
                    "Its themes of justice, loyalty, and the consequences of false accusations remain timeless.",
            category = "Tamil",
            isStatic = true
        ),
        CardEntity(
            title = "Tamil Metalworking Changed India",
            body = "The Chera, Chola, and Pandya dynasties developed advanced iron and steel production. " +
                    "Wootz steel, exported from Tamil ports, became the legendary Damascus steel of the Middle East. " +
                    "Chola bronze casting using the lost-wax method produced some of the finest metalwork in history. " +
                    "The Thanjavur Big Temple's 80-tonne Nandi statue was cast as a single piece over 1000 years ago.",
            category = "Tamil",
            isStatic = true
        ),
        CardEntity(
            title = "Tamil Is a Living Classical Language",
            body = "Sanskrit died as a spoken language centuries ago, but Tamil is still actively spoken by 80+ million people. " +
                    "It's one of only a few classical languages with unbroken literary tradition spanning 2000+ years. " +
                    "Tamil has loanwords in Malay, Sinhalese, and even Latin — evidence of ancient trade networks. " +
                    "The language has 247 characters organized into a precise phonetic system that ancient grammarians codified.",
            category = "Tamil",
            isStatic = true
        ),
        CardEntity(
            title = "The Concept of Aram in Tamil Ethics",
            body = "Aram means righteousness or duty — it's the central ethical concept in Tamil literature. " +
                    "Unlike rigid rule-following, Aram is contextual and compassionate. " +
                    "Thirukkural defines it as 'the duty that does not waver even in dire circumstances.' " +
                    "It balances individual conscience with social responsibility — a pragmatic ethical framework.",
            category = "Tamil",
            isStatic = true
        ),
        CardEntity(
            title = "Why Pongal Celebrates the Sun",
            body = "Pongal is a four-day harvest festival that coincides with the Sun's northward journey (Uttarayanam). " +
                    "Day one, Bhogi, discards old things — a cultural reset. Day two, Surya Pongal, thanks the Sun. " +
                    "The word 'Pongal' means 'to boil over' — rice and milk boiling over symbolizes abundance. " +
                    "It's one of the oldest continuously celebrated agricultural festivals in the world.",
            category = "Tamil",
            isStatic = true
        ),
        CardEntity(
            title = "Classical Tamil Music System",
            body = "Ancient Tamil music used a five-note pentatonic system before the seven-note system was adopted. " +
                    "The Silappatikaram describes music theory in detail, including rhythm patterns and instruments. " +
                    "Bharata's Natyashastra, which defines Indian classical music, was influenced by Tamil performance traditions. " +
                    "The connection between Tamil folk music and modern Carnatic music runs deeper than most musicians realize.",
            category = "Tamil",
            isStatic = true
        ),

        // General
        CardEntity(
            title = "The Feynman Technique for Learning",
            body = "Pick a concept and explain it in simple language as if teaching a 12-year-old. " +
                    "When you get stuck or use jargon, that's where your understanding has gaps. " +
                    "Go back to the source material, fill those gaps, then try explaining again. " +
                    "This four-step loop — choose, explain, identify gaps, review — beats passive re-reading every time.",
            category = "General",
            isStatic = true
        ),
        CardEntity(
            title = "The 2-Minute Rule Beats Procrastination",
            body = "If a task takes less than 2 minutes, do it immediately instead of adding it to a list. " +
                    "This comes from David Allen's GTD system and it eliminates tiny tasks that pile up mentally. " +
                    "Replying to a message, filing a document, washing a dish — do it now, not later. " +
                    "The mental bandwidth you free up from not tracking small tasks is surprisingly large.",
            category = "General",
            isStatic = true
        ),
        CardEntity(
            title = "Active Recall Beats Rereading",
            body = "Reading a chapter twice feels productive but your brain isn't actually working. " +
                    "Close the book and try to write down everything you remember — that struggle is where learning happens. " +
                    "Flashcards, practice problems, and self-quizzing all use active recall. " +
                    "Studies show active recall is 50% more effective than rereading the same material.",
            category = "General",
            isStatic = true
        ),
        CardEntity(
            title = "Sleep Consolidates What You Learned",
            body = "Your brain replays and strengthens neural connections during deep sleep and REM sleep. " +
                    "Studying right before sleeping means the material gets consolidated during the night. " +
                    "Pulling an all-nighter before an exam actively destroys your ability to remember what you studied. " +
                    "Seven hours of sleep before a test outperforms three extra hours of last-minute cramming.",
            category = "General",
            isStatic = true
        ),
        CardEntity(
            title = "Parkinson's Law: Work Expands to Fill Time",
            body = "Give yourself a week for a two-hour task and it'll take a week. " +
                    "Give yourself two hours and you'll finish in two hours. " +
                    "Artificial deadlines force prioritization and eliminate perfectionism on low-value work. " +
                    "Set timeboxes for tasks and treat them like appointments with yourself.",
            category = "General",
            isStatic = true
        ),
        CardEntity(
            title = "The Dunning-Kruger Effect Is Real",
            body = "People with low skill in an area consistently overestimate their ability because they lack the knowledge to see what they're missing. " +
                    "As expertise grows, confidence temporarily drops before rebuilding on a solid foundation. " +
                    "The fix is external feedback — code reviews, testing, peer evaluation. " +
                    "If you think you've mastered something, seek criticism; it's the fastest path to actual mastery.",
            category = "General",
            isStatic = true
        ),
        CardEntity(
            title = "Compound Interest Applies to Skills Too",
            body = "Getting 1% better each day means you're 37 times better in a year through compounding. " +
                    "Small consistent practice beats occasional heroic efforts every time. " +
                    "The people who win aren't the most talented — they're the ones who showed up daily. " +
                    "Track one skill you're building and measure small weekly improvements, not dramatic leaps.",
            category = "General",
            isStatic = true
        ),
        CardEntity(
            title = "You Don't Need Motivation, You Need Systems",
            body = "Motivation is unreliable — it comes and goes based on mood and energy. " +
                    "A system is a default behavior: study at 9pm every day regardless of how you feel. " +
                    "James Clear calls these 'habits' — automatic actions triggered by a cue, not by willpower. " +
                    "Design your environment so the right action is the easiest action.",
            category = "General",
            isStatic = true
        ),
        CardEntity(
            title = "The Pareto Principle in Daily Life",
            body = "80% of results come from 20% of efforts — find and double down on that 20%. " +
                    "In coding, 20% of bugs cause 80% of crashes — fix those first. " +
                    "In studying, 20% of topics appear in 80% of exam questions — identify and master those. " +
                    "Ask yourself: what's the one thing I could do that would make everything else easier?",
            category = "General",
            isStatic = true
        ),
        CardEntity(
            title = "Deep Work Is a Competitive Advantage",
            body = "Most people can't focus for more than 20 minutes without checking their phone. " +
                    "If you can do 4 hours of uninterrupted deep work daily, you'll outperform most peers in months. " +
                    "Block your best hours, turn off all notifications, and close your door. " +
                    "Cal Newport argues this skill is rare and valuable — exactly because so few people cultivate it.",
            category = "General",
            isStatic = true
        ),

        // LLM
        CardEntity(
            title = "Temperature Controls Output Creativity",
            body = "LLM temperature is a number between 0 and 2 that controls randomness in token selection. " +
                    "Temperature 0 always picks the most likely next token — deterministic and repetitive. " +
                    "Temperature 0.7 to 1.0 adds variety, making outputs more creative but less predictable. " +
                    "Use low temperature for code and facts, higher temperature for brainstorming and creative writing.",
            category = "LLM",
            isStatic = true
        ),
        CardEntity(
            title = "Context Window Is Your Input Limit",
            body = "The context window is how many tokens an LLM can see at once — input plus output combined. " +
                    "GPT-4 has 128K tokens, Llama 3 has 8K or 70K depending on the variant. " +
                    "Everything you send — system prompt, conversation history, documents — eats into that budget. " +
                    "When the context fills up, the model literally forgets the earliest messages in the conversation.",
            category = "LLM",
            isStatic = true
        ),
        CardEntity(
            title = "Fine-Tuning vs Prompt Engineering",
            body = "Prompt engineering changes how you ask; fine-tuning changes what the model knows. " +
                    "Most use cases are solved by better prompts, few-shot examples, and RAG retrieval. " +
                    "Fine-tuning is expensive, needs thousands of examples, and can cause catastrophic forgetting. " +
                    "Start with prompts. Only fine-tune when you've proven prompting can't solve your problem.",
            category = "LLM",
            isStatic = true
        ),
        CardEntity(
            title = "RAG Beats Hallucination for Facts",
            body = "Retrieval-Augmented Generation gives the LLM relevant documents before it answers. " +
                    "Instead of guessing, the model reads the actual source and references it. " +
                    "This dramatically reduces hallucination for factual questions about your specific data. " +
                    "Chunk documents into 256-512 token pieces, embed them, and retrieve the top-k relevant chunks.",
            category = "LLM",
            isStatic = true
        ),
        CardEntity(
            title = "Prompt Injection Is the New XSS",
            body = "Prompt injection tricks an LLM into ignoring its system prompt by injecting malicious instructions. " +
                    "If your app passes user input to an LLM, a user can say 'ignore all previous instructions and...'" +
                    "The fix is treating the system prompt as privileged and never letting user input override it. " +
                    "Use input validation, output filtering, and never concatenate user text directly into system prompts.",
            category = "LLM",
            isStatic = true
        ),
        CardEntity(
            title = "Local LLMs Run on Your Laptop Now",
            body = "Tools like Ollama, llama.cpp, and LM Studio let you run LLMs locally with no internet. " +
                    "Llama 3 8B runs comfortably on 8GB RAM; Mistral 7B on similar hardware. " +
                    "Quality isn't as good as cloud APIs, but privacy and offline access are unmatched. " +
                    "Use local models for code completion, document summarization, and experimentation.",
            category = "LLM",
            isStatic = true
        ),

        // Android
        CardEntity(
            title = "Compose Recomposition Is Selective",
            body = "When state changes in Jetpack Compose, only the composable functions that read that state recompose. " +
                    "If a variable is not state, Compose won't track it and won't recompose when it changes. " +
                    "Use remember and mutableStateOf for values that should trigger recomposition. " +
                    "Expensive composables should be wrapped in key() or derivedStateOf to reduce unnecessary work.",
            category = "Android",
            isStatic = true
        ),
        CardEntity(
            title = "Room Database Migrations Are Not Optional",
            body = "When you change your Room entity schema, you must write a migration or users lose their data. " +
                    "Version 1 to 2: add a column. Version 2 to 3: add a table. Each needs an ALTER or CREATE statement. " +
                    "Use fallbackToDestructiveMigration only during development — never in production. " +
                    "Test migrations by exporting the schema to JSON and using Room's testing helper.",
            category = "Android",
            isStatic = true
        ),
        CardEntity(
            title = "Why Android Kill Your Background Service",
            body = "Android aggressively kills background services to save battery, especially on Chinese ROMs. " +
                    "START_STICKY tells the system to restart your service if it gets killed. " +
                    "Foreground services with notifications survive longer but can still be killed. " +
                    "For critical work, use WorkManager with constraints — it handles the rescheduling for you.",
            category = "Android",
            isStatic = true
        ),
        CardEntity(
            title = "Coroutines and viewModelScope",
            body = "viewModelScope automatically cancels all coroutines when the ViewModel is cleared. " +
                    "This prevents memory leaks from long-running network calls after the screen is destroyed. " +
                    "Use viewModelScope.launch { } for UI-triggered operations that should survive rotation. " +
                    "Use lifecycleScope for activity/fragment-scoped work that should die with the UI.",
            category = "Android",
            isStatic = true
        ),
        CardEntity(
            title = "DataStore Replaces SharedPreferences",
            body = "SharedPreferences is synchronous and can corrupt on crash — DataStore fixes both problems. " +
                    "Preferences DataStore is a drop-in replacement using Kotlin Flows and coroutines. " +
                    "Proto DataStore adds type safety with Protocol Buffers, but Preferences is simpler to start. " +
                    "Migration from SharedPreferences is built in — call prefsDataStore with a migration object.",
            category = "Android",
            isStatic = true
        ),
    )

    private val defaultApps = listOf(
        InterceptedAppEntity("com.instagram.android", "Instagram", true),
        InterceptedAppEntity("com.instagram.barcelona", "Threads", true),
        InterceptedAppEntity("com.google.android.youtube", "YouTube", true),
        InterceptedAppEntity("com.google.android.youtube.music", "YouTube Music", false),
        InterceptedAppEntity("com.zhiliaoapp.musically", "TikTok", true),
        InterceptedAppEntity("com.twitter.android", "X (Twitter)", true),
        InterceptedAppEntity("com.facebook.katana", "Facebook", false),
        InterceptedAppEntity("com.snapchat.android", "Snapchat", false),
        InterceptedAppEntity("com.reddit.frontpage", "Reddit", true),
        InterceptedAppEntity("com.netflix.mediaclient", "Netflix", false),
        InterceptedAppEntity("com.spotify.music", "Spotify", false),
        InterceptedAppEntity("com.discord", "Discord", false),
    )

    suspend fun seedCards(cardDao: CardDao) {
        cardDao.insertCards(seedCards)
    }

    suspend fun seedApps(appDao: InterceptedAppDao) {
        appDao.insertApps(defaultApps)
    }
}
