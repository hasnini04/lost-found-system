<?php
header('Content-Type: application/json');

// --- DB connection ---
$host = 'localhost';
$db   = 'lost_and_found';
$user = 'root';
$pass = '';

$conn = new mysqli($host, $user, $pass, $db);
if ($conn->connect_error) {
    echo json_encode(["status" => "error", "message" => "DB connection failed"]);
    exit;
}

$action = isset($_GET['action']) ? $_GET['action'] : '';

if ($action === 'lost_items') {

    $sql = "
        SELECT 
            li.id,
            li.item_name,
            li.description,
            li.location_lost,
            li.date_lost,
            li.created_at,
            li.image_url,         -- relative path stored in DB (nullable)
            u.name AS reporter_name
        FROM lost_item li
        LEFT JOIN user u ON li.reporter_id = u.id
        ORDER BY li.created_at DESC
    ";

    $result = $conn->query($sql);
    $items = [];

    // Build base URL (safe + predictable)
    // Example result: http://localhost/lostfound/
    $scheme   = (!empty($_SERVER['HTTPS']) && $_SERVER['HTTPS'] !== 'off') ? 'https' : 'http';
    $hostName = $_SERVER['HTTP_HOST'];
    // SCRIPT_NAME -> /lostfound/admin_fetch_datalost.php
    // dirname(...) -> /lostfound
    $scriptDir = rtrim(str_replace('\\', '/', dirname($_SERVER['SCRIPT_NAME'])), '/');
    $webRoot   = $scheme . '://' . $hostName . $scriptDir . '/';

    // Filesystem root (DocumentRoot)
    // Use to verify file exists, but do not expose.
    $docRoot = rtrim(str_replace('\\', '/', $_SERVER['DOCUMENT_ROOT']), '/');

    if ($result) {
        while ($row = $result->fetch_assoc()) {
            $rel = $row['image_url']; // e.g. uploads/lost_images/lost_123.jpg

            if (!empty($rel)) {
                // Normalize relative (strip leading slash)
                $relNorm = ltrim(str_replace('\\', '/', $rel), '/');

                // Build full URL
                $fullUrl = $webRoot . $relNorm;

                // Optional existence check
                $fsPath = $docRoot . $scriptDir . '/' . $relNorm; // works when lostfound under docroot
                if (file_exists($fsPath)) {
                    $row['image_url_full'] = $fullUrl;
                } else {
                    // File missing: still return rel path but mark full null
                    $row['image_url_full'] = null;
                }

                // Put normalized relative back in case Java uses fallback
                $row['image_url'] = $relNorm;
            } else {
                $row['image_url_full'] = null;
            }

            $items[] = $row;
        }
    }

    echo json_encode([
        "status" => "success",
        "data"   => $items
    ], JSON_UNESCAPED_SLASHES);

} else {
    echo json_encode([
        "status"  => "error",
        "message" => "Invalid action"
    ]);
}

$conn->close();
