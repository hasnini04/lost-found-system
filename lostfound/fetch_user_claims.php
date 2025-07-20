<?php
header('Content-Type: text/plain'); // Java reads plain text lines

mysqli_report(MYSQLI_REPORT_ERROR | MYSQLI_REPORT_STRICT);

$host = 'localhost';
$db   = 'lost_and_found';
$user = 'root';
$pass = '';

$conn = new mysqli($host, $user, $pass, $db);
$conn->set_charset('utf8mb4');

$reporter_id = isset($_GET['reporter_id']) ? (int)$_GET['reporter_id'] : 0;
if ($reporter_id <= 0) {
    http_response_code(400);
    echo "Missing reporter_id";
    exit;
}

$sql = "
    SELECT
        c.id              AS claim_id,
        COALESCE(c.item_name, f.item_name, '') AS item_label,
        CASE 
            WHEN f.status = 'claimed' THEN 'claimed'
            ELSE c.status
        END               AS display_status,
        c.pickup_note     AS pickup_note,
        c.created_at      AS created_at,
        c.found_item_id   AS found_item_id
    FROM claim c
    LEFT JOIN found_item f ON f.id = c.found_item_id
    WHERE c.reporter_id = ?
    ORDER BY c.created_at DESC
";

$stmt = $conn->prepare($sql);
$stmt->bind_param("i", $reporter_id);
$stmt->execute();
$result = $stmt->get_result();

while ($row = $result->fetch_assoc()) {
    // Build pipe line: claim_id|item|status|pickup|created_at|found_item_id
    $line = implode('|', [
        $row['claim_id'],
        $row['item_label'],
        $row['display_status'],
        str_replace(["\r","\n","|"], [' ',' ','/'], (string)$row['pickup_note']),
        $row['created_at'],
        $row['found_item_id']
    ]);
    echo $line . "\n";
}

$stmt->close();
$conn->close();
