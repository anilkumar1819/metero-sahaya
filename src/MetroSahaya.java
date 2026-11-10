import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.sql.*;
import java.util.*;
import java.util.concurrent.Executors;

/**
 * Metro Sahaya - guided metro help for first-time travellers.
 * Java (JDK HttpServer + JDBC) + SQL (H2). No frameworks.
 * Version 2: passenger details, demo payment, printable ticket, ticket lookup.
 */
public class MetroSahaya {

    // ---------- in-memory copy of the network, loaded from SQL at startup ----------
    static Connection db;
    static final Map<Integer, String[]> LINES = new LinkedHashMap<>();   // id -> {en, hi, kn, color}
    static final Map<Integer, List<Integer>> SEQ = new HashMap<>();      // line -> ordered station ids
    static final Map<Integer, String> STATION = new LinkedHashMap<>();   // station id -> name
    static final int MIN_PER_STOP = 2, MIN_PER_CHANGE = 5, FREE_BELOW_AGE = 5;
    static final SecureRandom RND = new SecureRandom();

    // ---------- localised sentence templates ----------
    static final Map<String, String> BOARD = Map.of(
        "en", "Board the %1$s train towards %2$s at %3$s. Ride %4$d stops and get off at %5$s.",
        "hi", "%3$s से %1$s की ट्रेन में चढ़ें (%2$s की ओर)। %4$d स्टॉप के बाद %5$s पर उतरें।",
        "kn", "%3$s ನಿಲ್ದಾಣದಲ್ಲಿ %2$s ಕಡೆಗೆ ಹೋಗುವ %1$s ರೈಲು ಹತ್ತಿ. %4$d ನಿಲ್ದಾಣಗಳ ನಂತರ %5$s ನಲ್ಲಿ ಇಳಿಯಿರಿ.");
    static final Map<String, String> CHANGE = Map.of(
        "en", "Change trains: follow the signs to the %1$s platform.",
        "hi", "ट्रेन बदलें: %1$s के प्लेटफॉर्म के संकेत देखें।",
        "kn", "ರೈಲು ಬದಲಾಯಿಸಿ: %1$s ಪ್ಲಾಟ್‌ಫಾರ್ಮ್‌ನ ಸೂಚನಾ ಫಲಕ ಅನುಸರಿಸಿ.");
    static final Map<String, String> SUMMARY = Map.of(
        "en", "%1$d stops · about %2$d minutes · fare ₹%3$d",
        "hi", "%1$d स्टॉप · लगभग %2$d मिनट · किराया ₹%3$d",
        "kn", "%1$d ನಿಲ್ದಾಣಗಳು · ಸುಮಾರು %2$d ನಿಮಿಷ · ದರ ₹%3$d");

    public static void main(String[] args) throws Exception {
        db = DriverManager.getConnection("jdbc:h2:./data/metro;DB_CLOSE_DELAY=-1", "sa", "");
        initDatabase();
        loadNetwork();

        int port = args.length > 0 ? Integer.parseInt(args[0])
                 : Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));   // hosts set PORT
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", MetroSahaya::serveHome);
        api(server, "/api/stations", q -> stationsJson());
        api(server, "/api/guide",    q -> guideJson(lang(q)));
        api(server, "/api/route",    q -> routeJson(id(q, "from"), id(q, "to"), lang(q)));
        api(server, "/api/book",     MetroSahaya::book);                       // POST
        api(server, "/api/ticket",   q -> ticketJson(need(q, "code"), need(q, "phone4")));
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();
        System.out.println("Metro Sahaya running at http://localhost:" + port);
    }

    // ---------- database ----------
    static boolean tableExists(Statement s, String name) throws SQLException {
        try (ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = '" + name + "'")) {
            rs.next();
            return rs.getInt(1) > 0;
        }
    }

    static void initDatabase() throws SQLException {
        try (Statement s = db.createStatement()) {
            if (!tableExists(s, "LINES")) {
                s.execute("RUNSCRIPT FROM 'sql/schema.sql' CHARSET 'UTF-8'");
                s.execute("RUNSCRIPT FROM 'sql/seed.sql' CHARSET 'UTF-8'");
                System.out.println("Database created and seeded.");
            } else if (!tableExists(s, "PASSENGERS")) {          // database made by version 1
                s.execute("RUNSCRIPT FROM 'sql/upgrade.sql' CHARSET 'UTF-8'");
                System.out.println("Database upgraded (passenger details added).");
            }
        }
    }

    static void loadNetwork() throws SQLException {
        try (Statement s = db.createStatement()) {
            ResultSet r = s.executeQuery("SELECT id, name, name_hi, name_kn, color FROM lines ORDER BY id");
            while (r.next()) LINES.put(r.getInt(1), new String[]{r.getString(2), r.getString(3), r.getString(4), r.getString(5)});
            r = s.executeQuery("SELECT id, name FROM stations ORDER BY name");
            while (r.next()) STATION.put(r.getInt(1), r.getString(2));
            r = s.executeQuery("SELECT line_id, station_id FROM line_stations ORDER BY line_id, seq");
            while (r.next()) SEQ.computeIfAbsent(r.getInt(1), k -> new ArrayList<>()).add(r.getInt(2));
        }
    }

    // ---------- route planning: Dijkstra over (line, position) states ----------
    record Node(int line, int idx) {}
    record Item(Node node, int dist) {}
    record Leg(int line, int fromStn, int toStn, int stops, String towards) {}
    record Plan(List<Leg> legs, int stops, int minutes, int fare) {}

    static Plan plan(int from, int to) throws SQLException {
        if (!STATION.containsKey(from) || !STATION.containsKey(to)) throw new IllegalArgumentException("Unknown station");
        if (from == to) throw new IllegalArgumentException("Start and destination are the same station");

        Map<Node, Integer> dist = new HashMap<>();
        Map<Node, Node> prev = new HashMap<>();
        PriorityQueue<Item> pq = new PriorityQueue<>(Comparator.comparingInt(Item::dist));
        for (var e : SEQ.entrySet()) {
            int i = e.getValue().indexOf(from);
            if (i >= 0) { Node n = new Node(e.getKey(), i); dist.put(n, 0); pq.add(new Item(n, 0)); }
        }
        while (!pq.isEmpty()) {
            Item it = pq.poll();
            if (it.dist() > dist.getOrDefault(it.node(), Integer.MAX_VALUE)) continue;
            List<Integer> seq = SEQ.get(it.node().line());
            int stn = seq.get(it.node().idx());
            List<Node> next = new ArrayList<>();
            List<Integer> cost = new ArrayList<>();
            for (int d : new int[]{-1, 1}) {                       // ride one stop either way
                int j = it.node().idx() + d;
                if (j >= 0 && j < seq.size()) { next.add(new Node(it.node().line(), j)); cost.add(MIN_PER_STOP); }
            }
            for (var e : SEQ.entrySet()) {                         // change to another line here
                int j = e.getValue().indexOf(stn);
                if (e.getKey() != it.node().line() && j >= 0) { next.add(new Node(e.getKey(), j)); cost.add(MIN_PER_CHANGE); }
            }
            for (int k = 0; k < next.size(); k++) {
                int nd = it.dist() + cost.get(k);
                if (nd < dist.getOrDefault(next.get(k), Integer.MAX_VALUE)) {
                    dist.put(next.get(k), nd); prev.put(next.get(k), it.node()); pq.add(new Item(next.get(k), nd));
                }
            }
        }
        Node end = null;
        for (var e : SEQ.entrySet()) {
            int j = e.getValue().indexOf(to);
            if (j >= 0) {
                Node n = new Node(e.getKey(), j);
                if (dist.containsKey(n) && (end == null || dist.get(n) < dist.get(end))) end = n;
            }
        }
        if (end == null) throw new IllegalArgumentException("No route found");

        LinkedList<Node> path = new LinkedList<>();
        for (Node n = end; n != null; n = prev.get(n)) path.addFirst(n);

        List<Leg> legs = new ArrayList<>();                        // one leg per line ridden
        Node start = path.getFirst();
        for (int i = 1; i <= path.size(); i++) {
            boolean last = i == path.size();
            if (last || path.get(i).line() != path.get(i - 1).line()) {
                Node fin = path.get(i - 1);
                List<Integer> seq = SEQ.get(start.line());
                int stops = Math.abs(fin.idx() - start.idx());
                if (stops > 0) {
                    int term = fin.idx() > start.idx() ? seq.get(seq.size() - 1) : seq.get(0);
                    legs.add(new Leg(start.line(), seq.get(start.idx()), seq.get(fin.idx()), stops, STATION.get(term)));
                }
                if (!last) start = path.get(i);
            }
        }
        int totalStops = legs.stream().mapToInt(Leg::stops).sum();
        return new Plan(legs, totalStops, dist.get(end), fareFor(totalStops));
    }

    static String routeJson(int from, int to, String lang) throws SQLException {
        Plan p = plan(from, to);
        int li = langIndex(lang);
        StringBuilder sb = new StringBuilder("{\"from\":").append(Json.s(STATION.get(from)))
            .append(",\"to\":").append(Json.s(STATION.get(to)))
            .append(",\"stops\":").append(p.stops())
            .append(",\"minutes\":").append(p.minutes())
            .append(",\"fare\":").append(p.fare())
            .append(",\"summary\":").append(Json.s(String.format(Locale.ROOT, SUMMARY.get(lang), p.stops(), p.minutes(), p.fare())))
            .append(",\"legs\":[");
        for (int i = 0; i < p.legs().size(); i++) {
            Leg l = p.legs().get(i);
            String[] ln = LINES.get(l.line());
            String board = String.format(Locale.ROOT, BOARD.get(lang), ln[li], l.towards(), STATION.get(l.fromStn()), l.stops(), STATION.get(l.toStn()));
            String change = i == 0 ? "" : String.format(Locale.ROOT, CHANGE.get(lang), ln[li]);
            if (i > 0) sb.append(',');
            sb.append("{\"line\":").append(Json.s(ln[0])).append(",\"color\":").append(Json.s(ln[3]))
              .append(",\"towards\":").append(Json.s(l.towards()))
              .append(",\"from\":").append(Json.s(STATION.get(l.fromStn())))
              .append(",\"to\":").append(Json.s(STATION.get(l.toStn())))
              .append(",\"stops\":").append(l.stops())
              .append(",\"change\":").append(Json.s(change))
              .append(",\"text\":").append(Json.s(board)).append('}');
        }
        return sb.append("]}").toString();
    }

    static int fareFor(int stops) throws SQLException {
        try (PreparedStatement ps = db.prepareStatement("SELECT fare FROM fare_slabs WHERE ? BETWEEN min_stops AND max_stops")) {
            ps.setInt(1, stops);
            ResultSet r = ps.executeQuery();
            if (!r.next()) throw new IllegalStateException("No fare slab for " + stops + " stops");
            return r.getInt(1);
        }
    }

    // ---------- booking: validate -> one SQL transaction -> return the ticket ----------
    static String book(Map<String, String> q) throws Exception {
        if (!"POST".equals(q.get("_method"))) throw new IllegalArgumentException("Booking must use POST");
        int from = id(q, "from"), to = id(q, "to");
        Plan p = plan(from, to);

        String contact = cleanName(q.get("contact_name"), "Contact name");
        String phone = q.getOrDefault("phone", "").trim();
        if (!phone.matches("[6-9]\\d{9}")) throw new IllegalArgumentException("Enter a valid 10-digit mobile number");
        String pay = q.getOrDefault("pay", "").toUpperCase(Locale.ROOT);
        if (!List.of("UPI", "CARD", "CASH").contains(pay)) throw new IllegalArgumentException("Choose a payment method");
        int n;
        try { n = Integer.parseInt(q.getOrDefault("n", "")); } catch (NumberFormatException e) { n = 0; }
        if (n < 1 || n > 6) throw new IllegalArgumentException("Passengers must be 1 to 6");

        List<Object[]> pax = new ArrayList<>();                    // {name, age, gender, fare}
        int total = 0;
        for (int i = 1; i <= n; i++) {
            String name = cleanName(q.get("name" + i), "Passenger " + i + " name");
            int age;
            try { age = Integer.parseInt(q.getOrDefault("age" + i, "")); } catch (NumberFormatException e) { age = -1; }
            if (age < 0 || age > 120) throw new IllegalArgumentException("Passenger " + i + " age must be 0 to 120");
            String g = q.getOrDefault("gender" + i, "").toUpperCase(Locale.ROOT);
            if (!List.of("M", "F", "O").contains(g)) throw new IllegalArgumentException("Choose gender for passenger " + i);
            int fare = age < FREE_BELOW_AGE ? 0 : p.fare();         // sample rule: under 5 travel free
            total += fare;
            pax.add(new Object[]{name, age, g, fare});
        }
        String status = pay.equals("CASH") ? "PAY_AT_COUNTER" : "PAID";   // demo: UPI/card "succeed"

        synchronized (db) {
            boolean oldAuto = db.getAutoCommit();
            db.setAutoCommit(false);
            try {
                long ticketId = -1; String code = null;
                for (int attempt = 0; attempt < 5 && ticketId < 0; attempt++) {
                    code = String.format("MS%08d", RND.nextInt(100_000_000));
                    try (PreparedStatement ps = db.prepareStatement(
                            "INSERT INTO tickets(code, from_station, to_station, passengers, total_fare, contact_name, phone, payment_method, status) "
                          + "VALUES (?,?,?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
                        ps.setString(1, code); ps.setInt(2, from); ps.setInt(3, to); ps.setInt(4, n); ps.setInt(5, total);
                        ps.setString(6, contact); ps.setString(7, phone); ps.setString(8, pay); ps.setString(9, status);
                        ps.executeUpdate();
                        ResultSet k = ps.getGeneratedKeys(); k.next(); ticketId = k.getLong(1);
                    } catch (SQLException e) { if (!"23505".equals(e.getSQLState())) throw e; }   // duplicate code -> retry
                }
                if (ticketId < 0) throw new IllegalStateException("Could not create ticket");
                try (PreparedStatement ps = db.prepareStatement("INSERT INTO passengers(ticket_id, name, age, gender, fare) VALUES (?,?,?,?,?)")) {
                    for (Object[] o : pax) {
                        ps.setLong(1, ticketId); ps.setString(2, (String) o[0]); ps.setInt(3, (Integer) o[1]);
                        ps.setString(4, (String) o[2]); ps.setInt(5, (Integer) o[3]);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                db.commit();
                return ticketJson(code, null);
            } catch (Exception e) {
                db.rollback();
                throw e;
            } finally {
                db.setAutoCommit(oldAuto);
            }
        }
    }

    /** phone4 == null: caller just booked it (no check). Otherwise the last 4 phone digits must match. */
    static String ticketJson(String code, String phone4) throws SQLException {
        String sql = "SELECT t.id, t.code, f.name, d.name, t.total_fare, t.status, t.payment_method, t.contact_name, t.phone, t.travel_date "
                   + "FROM tickets t JOIN stations f ON f.id = t.from_station JOIN stations d ON d.id = t.to_station WHERE t.code = ?";
        try (PreparedStatement ps = db.prepareStatement(sql)) {
            ps.setString(1, code.trim().toUpperCase(Locale.ROOT));
            ResultSet r = ps.executeQuery();
            if (!r.next()) throw new IllegalArgumentException("Ticket not found, or the phone digits do not match");
            String phone = r.getString(9);
            if (phone4 != null && !(phone.length() == 10 && phone.endsWith(phone4.trim())))
                throw new IllegalArgumentException("Ticket not found, or the phone digits do not match");
            StringBuilder sb = new StringBuilder("{\"code\":").append(Json.s(r.getString(2)))
                .append(",\"from\":").append(Json.s(r.getString(3))).append(",\"to\":").append(Json.s(r.getString(4)))
                .append(",\"total\":").append(r.getInt(5)).append(",\"status\":").append(Json.s(r.getString(6)))
                .append(",\"payment\":").append(Json.s(r.getString(7))).append(",\"contact\":").append(Json.s(r.getString(8)))
                .append(",\"phone\":").append(Json.s(phone.length() == 10 ? "XXXXXX" + phone.substring(6) : ""))
                .append(",\"date\":").append(Json.s(r.getString(10))).append(",\"passengers\":[");
            try (PreparedStatement pp = db.prepareStatement("SELECT name, age, gender, fare FROM passengers WHERE ticket_id = ? ORDER BY id")) {
                pp.setLong(1, r.getLong(1));
                ResultSet pr = pp.executeQuery();
                boolean first = true;
                while (pr.next()) {
                    if (!first) sb.append(','); first = false;
                    sb.append("{\"name\":").append(Json.s(pr.getString(1))).append(",\"age\":").append(pr.getInt(2))
                      .append(",\"gender\":").append(Json.s(pr.getString(3))).append(",\"fare\":").append(pr.getInt(4)).append('}');
                }
            }
            return sb.append("]}").toString();
        }
    }

    static String cleanName(String v, String label) {
        v = v == null ? "" : v.trim().replaceAll("\\s+", " ");
        if (v.isEmpty() || v.length() > 60 || !v.matches("[\\p{L}\\p{M} .'-]+"))
            throw new IllegalArgumentException(label + " is missing or has invalid characters");
        return v;
    }

    // ---------- other endpoints ----------
    static String stationsJson() throws SQLException {
        String sql = "SELECT s.id, s.name, s.landmark, (s.id IN (SELECT station_id FROM interchanges)) AS inter "
                   + "FROM stations s ORDER BY s.name";
        StringBuilder sb = new StringBuilder("[");
        try (Statement st = db.createStatement(); ResultSet r = st.executeQuery(sql)) {
            boolean first = true;
            while (r.next()) {
                if (!first) sb.append(','); first = false;
                sb.append("{\"id\":").append(r.getInt(1)).append(",\"name\":").append(Json.s(r.getString(2)))
                  .append(",\"landmark\":").append(Json.s(r.getString(3)))
                  .append(",\"interchange\":").append(r.getBoolean(4)).append('}');
            }
        }
        return sb.append(']').toString();
    }

    static String guideJson(String lang) throws SQLException {
        StringBuilder sb = new StringBuilder("[");
        try (PreparedStatement ps = db.prepareStatement("SELECT step_no, title, body FROM guide_steps WHERE lang = ? ORDER BY step_no")) {
            ps.setString(1, lang);
            ResultSet r = ps.executeQuery();
            boolean first = true;
            while (r.next()) {
                if (!first) sb.append(','); first = false;
                sb.append("{\"step\":").append(r.getInt(1)).append(",\"title\":").append(Json.s(r.getString(2)))
                  .append(",\"body\":").append(Json.s(r.getString(3))).append('}');
            }
        }
        return sb.append(']').toString();
    }

    // ---------- tiny HTTP helpers ----------
    interface Handler { String handle(Map<String, String> q) throws Exception; }

    static void api(HttpServer server, String path, Handler h) {
        server.createContext(path, ex -> {
            int status = 200; String body;
            try { body = h.handle(params(ex)); }
            catch (IllegalArgumentException e) { status = 400; body = "{\"error\":" + Json.s(e.getMessage()) + "}"; }
            catch (Exception e) { status = 500; body = "{\"error\":" + Json.s("Server error") + "}"; e.printStackTrace(); }
            send(ex, status, "application/json; charset=utf-8", body.getBytes(StandardCharsets.UTF_8));
        });
    }

    static void serveHome(HttpExchange ex) throws IOException {
        if (!ex.getRequestURI().getPath().equals("/")) { send(ex, 404, "text/plain", "Not found".getBytes()); return; }
        send(ex, 200, "text/html; charset=utf-8", Files.readAllBytes(Path.of("web/index.html")));
    }

    static void send(HttpExchange ex, int status, String type, byte[] body) throws IOException {
        ex.getResponseHeaders().set("Content-Type", type);
        ex.sendResponseHeaders(status, body.length);
        ex.getResponseBody().write(body);
        ex.close();
    }

    /** Merges URL query parameters and (for POST) a form-encoded body into one map. */
    static Map<String, String> params(HttpExchange ex) throws IOException {
        Map<String, String> m = new HashMap<>();
        parseInto(m, ex.getRequestURI().getRawQuery());
        if ("POST".equals(ex.getRequestMethod()))
            parseInto(m, new String(ex.getRequestBody().readNBytes(20_000), StandardCharsets.UTF_8));
        m.put("_method", ex.getRequestMethod());
        return m;
    }

    static void parseInto(Map<String, String> m, String raw) {
        if (raw == null || raw.isEmpty()) return;
        for (String p : raw.split("&")) {
            String[] kv = p.split("=", 2);
            m.put(URLDecoder.decode(kv[0], StandardCharsets.UTF_8), kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "");
        }
    }

    static int id(Map<String, String> q, String key) {
        try { return Integer.parseInt(q.get(key)); } catch (Exception e) { throw new IllegalArgumentException("Missing or bad '" + key + "'"); }
    }
    static String need(Map<String, String> q, String key) {
        String v = q.get(key);
        if (v == null || v.isBlank()) throw new IllegalArgumentException("Missing '" + key + "'");
        return v;
    }
    static String lang(Map<String, String> q) { String l = q.getOrDefault("lang", "en"); return BOARD.containsKey(l) ? l : "en"; }
    static int langIndex(String l) { return l.equals("hi") ? 1 : l.equals("kn") ? 2 : 0; }

    static class Json {
        static String s(String v) {
            if (v == null) return "null";
            StringBuilder b = new StringBuilder("\"");
            for (char c : v.toCharArray()) {
                switch (c) {
                    case '"' -> b.append("\\\"");
                    case '\\' -> b.append("\\\\");
                    case '\n' -> b.append("\\n");
                    case '\r' -> b.append("\\r");
                    case '\t' -> b.append("\\t");
                    default -> { if (c < 0x20) b.append(String.format("\\u%04x", (int) c)); else b.append(c); }
                }
            }
            return b.append('"').toString();
        }
    }
}
