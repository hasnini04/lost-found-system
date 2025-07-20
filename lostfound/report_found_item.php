<?php
header('Content-Type: application/json; charset=utf-8');

/*
 * report_found_item.php
 * ---------------------
 * Expected POST:
 *   item_name      (string, required)
 *   description    (string, required)
 *   location       (string, required if no fallback file)
 *   date           (yyyy-mm-dd, required)
 *   reporter_id    (ignored by found_item schema but accepted)
 *   image          (file, optional)
 *
 * Behavior:
 *   • Cleans the location (strip tags, remove Plus Code like "(8889+5R)", collapse whitespace).
 *   • Uses only the first meaningful part of address (before first comma) so DB stores a short place name.
 *   • Inserts row into found_item w/ status='unclaimed'.
 *   • Stores uploaded image in /uploads_found/, saves relative path in DB.
 *   • If location missing in POST, will try fallback from latest_location.txt (map selection temp store).
 */

$host = "localhost";
$dbname = "lost_and_found";
$username = "root";
$password = "";

/* ------------------------------------------------------------
 * DB connect
 * ------------------------------------------------------------ */
$conn = new mysqli($host, $username, $password, $dbname);
if ($conn->connect_error) {
    echo json_encode(["status" => "error", "message" => "Database connection failed"]);
    exit;
}

/* ------------------------------------------------------------
 * Collect + sanitize input
 * ------------------------------------------------------------ */
$item_name      = trim($_POST['item_name']   ?? '');
$description    = trim($_POST['description'] ?? '');
$location_input = trim($_POST['location']    ?? '');
$date_found     = trim($_POST['date']        ?? '');
$reporter_id    = trim($_POST['reporter_id'] ?? ''); // not used in this table

// Fallback: if location empty, try latest_location.txt (map temp)
if ($location_input === '' && is_file(__DIR__ . '/latest_location.txt')) {
    $location_input = trim(file_get_contents(__DIR__ . '/latest_location.txt'));
}

$location_found = clean_location($location_input);

/* Validate required fields */
if ($item_name === '' || $description === '' || $location_found === '' || $date_found === '') {
    echo json_encode(["status" => "error", "message" => "Missing fields"]);
    exit;
}

/* Basic date sanity */
if (!preg_match('/^\d{4}-\d{2}-\d{2}$/', $date_found)) {
    echo json_encode(["status" => "error", "message" => "Invalid date format (yyyy-mm-dd)"]);
    exit;
}

/* ------------------------------------------------------------
 * Optional image upload
 * ------------------------------------------------------------ */
$image_path = null; // relative path stored in DB

if (!empty($_FILES['image']['name']) && is_uploaded_file($_FILES['image']['tmp_name'])) {
    $uploads_dir_fs  = __DIR__ . '/uploads_found'; // physical folder
    $uploads_dir_rel = 'uploads_found';            // relative for web/DB

    if (!is_dir($uploads_dir_fs)) {
        mkdir($uploads_dir_fs, 0775, true);
    }

    $tmp_name  = $_FILES['image']['tmp_name'];
    $orig_name = basename($_FILES['image']['name']);
    $ext       = strtolower(pathinfo($orig_name, PATHINFO_EXTENSION));
    $allowed   = ['jpg','jpeg','png','gif','bmp','webp'];

    if (!in_array($ext, $allowed)) {
        echo json_encode(["status" => "error", "message" => "Unsupported image type"]);
        exit;
    }

    $new_name   = uniqid('found_', true) . '.' . $ext;
    $target_fs  = $uploads_dir_fs  . '/' . $new_name;
    $target_rel = $uploads_dir_rel . '/' . $new_name;

    if (!move_uploaded_file($tmp_name, $target_fs)) {
        echo json_encode(["status" => "error", "message" => "Image upload failed"]);
        exit;
    }

    $image_path = $target_rel;
}

/* ------------------------------------------------------------
 * Insert row
 * ------------------------------------------------------------ */
/* Two SQL paths: with image vs. without image (clean NULL behavior) */
if ($image_path !== null) {
    $sql = "
        INSERT INTO found_item
            (item_name, description, image_path, location_found, date_found, status, created_at)
        VALUES
            (?, ?, ?, ?, ?, 'unclaimed', NOW())
    ";
    $stmt = $conn->prepare($sql);
    if (!$stmt) {
        echo json_encode(["status" => "error", "message" => $conn->error]);
        $conn->close();
        exit;
    }
    $stmt->bind_param("sssss", $item_name, $description, $image_path, $location_found, $date_found);
} else {
    $sql = "
        INSERT INTO found_item
            (item_name, description, location_found, date_found, status, created_at)
        VALUES
            (?, ?, ?, ?, 'unclaimed', NOW())
    ";
    $stmt = $conn->prepare($sql);
    if (!$stmt) {
        echo json_encode(["status" => "error", "message" => $conn->error]);
        $conn->close();
        exit;
    }
    $stmt->bind_param("ssss", $item_name, $description, $location_found, $date_found);
}

/* ------------------------------------------------------------
 * Execute + respond
 * ------------------------------------------------------------ */
if ($stmt->execute()) {
    $newId = $stmt->insert_id;
    echo json_encode([
        "status"     => "success",
        "message"    => "Item reported",
        "id"         => $newId,
        "status_db"  => "unclaimed",
        "image"      => $image_path,
        "location"   => $location_found
    ]);
} else {
    echo json_encode(["status" => "error", "message" => $stmt->error]);
}

$stmt->close();
$conn->close();

/* ------------------------------------------------------------
 * Helpers
 * ------------------------------------------------------------ */
function clean_location(string $s): string {
    // strip HTML tags
    $s = strip_tags($s);

    // collapse newlines/tabs -> spaces
    $s = preg_replace('/[\r\n\t]+/', ' ', $s);

    // trim
    $s = trim($s);

    // remove leading Plus Code in parentheses e.g. "(8889+5R) Something"
    if (preg_match('/^\(([A-Z0-9\+\-]{4,})\)\s*(.*)$/i', $s, $m)) {
        $s = trim($m[2]);
    }

    // take only text before first comma (we want solid place name)
    $s = explode(',', $s, 2)[0];

    // enforce DB column limit (location_found VARCHAR(255))
    if (strlen($s) > 255) {
        $s = substr($s, 0, 255);
    }

    return $s;
}
