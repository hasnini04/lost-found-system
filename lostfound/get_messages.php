<?php
/**
 * get_messages.php
 * ----------------
 * Fetch all messages for a reporter (user) and return either:
 *   • Plain line format: "YYYY-MM-DD HH:MM:SS|Message"  (default; matches current UserInboxPage loader)
 *   • JSON: {status:..., data:[...]}  (use ?format=json)
 *
 * Required GET:
 *   userId=<reporter_user_id>
 *
 * Optional GET:
 *   format=json    -> return JSON instead of line format
 */

header("Content-Type: text/plain; charset=utf-8"); // default; may change below if JSON requested

// ---------------- DB connect ----------------
$host = "localhost";
$dbname = "lost_and_found";
$user = "root";
$pass = "";

try {
    $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8mb4", $user, $pass, [
        PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
    ]);
} catch (PDOException $e) {
    http_response_code(500);
    echo "ERROR|DB connection failed: ".$e->getMessage();
    exit;
}

// ---------------- Inputs ----------------
$reporterId = isset($_GET['userId']) ? (int)$_GET['userId'] : 0; // param name matches your Java call
if ($reporterId <= 0) {
    http_response_code(400);
    echo "ERROR|Missing or invalid userId";
    exit;
}

$wantJson = (isset($_GET['format']) && strtolower($_GET['format']) === 'json');

// ---------------- Query ----------------
$sql = "SELECT m.id,
               m.message,
               m.created_at,
               m.is_read,
               m.item_id,
               a.name AS admin_name
        FROM messages m
        LEFT JOIN user a ON a.id = m.admin_id
        WHERE m.reporter_id = :rid
        ORDER BY m.created_at DESC";

$stmt = $pdo->prepare($sql);
$stmt->execute([':rid' => $reporterId]);
$rows = $stmt->fetchAll(PDO::FETCH_ASSOC);

// ---------------- Output: JSON? ----------------
if ($wantJson) {
    header("Content-Type: application/json; charset=utf-8");
    echo json_encode([
        "status" => "success",
        "data"   => $rows
    ]);
    exit;
}

// ---------------- Output: line format ----------------
// Each line: created_at|message   (newlines collapsed; pipe chars removed)
foreach ($rows as $r) {
    $date = $r['created_at'];
    $msg  = $r['message'];

    // sanitize for single-line pipe-delimited output
    $msg = str_replace(["\r", "\n", "|"], " ", $msg);

    echo $date . "|" . $msg . "\n";
}
