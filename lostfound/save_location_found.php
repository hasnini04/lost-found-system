<?php
// save_location_found.php
// Receives: POST address=... [&found_item_id=123]
// Stores clean place name only. No lat/lng.

// -------------------------------------
// Input
// -------------------------------------
header("Content-Type: text/plain; charset=UTF-8");

$address = isset($_POST['address']) ? trim($_POST['address']) : '';
$foundId = isset($_POST['found_item_id']) ? (int)$_POST['found_item_id'] : 0;

if ($address === '') {
    http_response_code(400);
    echo "ERR_NO_ADDRESS";
    exit;
}

// Clean the address (remove Plus Code, HTML, extra parts)
$clean = clean_address($address);

// If nothing left after cleaning, bail
if ($clean === '') {
    http_response_code(400);
    echo "ERR_BAD_ADDRESS";
    exit;
}

// -------------------------------------
// If found_item_id provided: update DB
// -------------------------------------
if ($foundId > 0) {
    $host = "localhost";
    $dbname = "lost_and_found";
    $user = "root";
    $pass = "";

    try {
        $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8mb4", $user, $pass);
        $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

        $stmt = $pdo->prepare("UPDATE found_item SET location_found = :addr WHERE id = :id");
        $stmt->execute([
            ':addr' => $clean,
            ':id'   => $foundId
        ]);

        echo "OK_DB";
        exit;
    } catch (PDOException $e) {
        http_response_code(500);
        echo "ERR_DB";
        exit;
    }
}

// -------------------------------------
// No found_item_id: write to temp file
// (Used by map -> Java form -> report_found_item.php flow.)
// -------------------------------------
$file = __DIR__ . "/latest_location.txt";
if (file_put_contents($file, $clean) !== false) {
    echo "OK_TMP";
} else {
    http_response_code(500);
    echo "ERR_WRITE";
}

// -------------------------------------
// Helpers
// -------------------------------------
function clean_address(string $s): string {
    // Strip HTML tags
    $s = strip_tags($s);

    // Collapse newlines/tabs to spaces
    $s = preg_replace('/[\r\n\t]+/', ' ', $s);

    // Trim whitespace
    $s = trim($s);

    // Remove leading Plus Code in parentheses, e.g. "(8878+Q9) Something"
    if (preg_match('/^\(([A-Z0-9\+\-]{4,})\)\s*(.*)$/i', $s, $m)) {
        $s = trim($m[2]);
    }

    // Some Plus Codes appear without parentheses at the start: "8878+Q9 Durian Tunggal"
    if (preg_match('/^[A-Z0-9\+\-]{4,}\s+(.*)$/i', $s, $m)) {
        $s = trim($m[1]);
    }

    // Use only the first comma-delimited part (place name)
    $s = explode(',', $s, 2)[0];

    // Enforce 255-char limit (DB column)
    if (strlen($s) > 255) {
        $s = substr($s, 0, 255);
    }

    return $s;
}
