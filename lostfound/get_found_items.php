<?php
/**
 * get_found_items.php
 * -------------------
 * Returns found items + latest claim status.
 *
 * Output modes:
 *   ?format=json  -> JSON {status,data:[...]}
 *   (default)     -> pipe rows:
 *                    id|item_name|description|location_found|date_found|item_status|created_at|image_path|claim_status
 *
 * claim_status is derived from the most recent row in `claim` for that found_item_id.
 * If no claim rows exist, claim_status = 'none'.
 */

$host = 'localhost';
$db   = 'lost_and_found';
$user = 'root';
$pass = '';

$format = isset($_GET['format']) ? strtolower($_GET['format']) : '';

/* ---------- DB connect ---------- */
$conn = new mysqli($host, $user, $pass, $db);
if ($conn->connect_error) {
    if ($format === 'json') {
        header('Content-Type: application/json');
        echo json_encode(["status" => "error", "message" => "DB connection failed"]);
    } else {
        header('Content-Type: text/plain; charset=utf-8');
        echo "error|DB connection failed\n";
    }
    exit;
}

/* ---------- Build absolute base URL for images ---------- */
$scheme    = (!empty($_SERVER['HTTPS']) && $_SERVER['HTTPS'] !== 'off') ? 'https' : 'http';
$hostName  = $_SERVER['HTTP_HOST'] ?? 'localhost';
$scriptDir = rtrim(str_replace('\\','/', dirname($_SERVER['SCRIPT_NAME'] ?? '')), '/');
$baseUrl   = $scheme . '://' . $hostName . $scriptDir . '/';

/* ---------- Query: found_item + latest claim status ---------- */
/*
   We pull each found_item, plus the status of its most recent claim (if any).
   claim.status ENUM('pending','approved','rejected'); may be NULL if no claim.
*/
$sql = "
    SELECT
        fi.id,
        fi.item_name,
        fi.description,
        fi.location_found,
        fi.date_found,
        fi.status            AS item_status_raw,
        fi.created_at,
        fi.image_path,
        COALESCE((
            SELECT c.status
            FROM claim c
            WHERE c.found_item_id = fi.id
            ORDER BY c.created_at DESC
            LIMIT 1
        ), 'none')           AS claim_status
    FROM found_item fi
    ORDER BY fi.created_at DESC
";

$res = $conn->query($sql);

$data = [];
if ($res) {
    while ($row = $res->fetch_assoc()) {
        // Normalize item_status: treat blank/NULL as 'unclaimed'
        $itemStatus = trim((string)$row['item_status_raw']);
        if ($itemStatus === '') {
            $itemStatus = 'unclaimed';
        }

        // Normalize image path + full URL
        $rel = $row['image_path'];
        if (!empty($rel)) {
            $relNorm = ltrim(str_replace('\\','/',$rel), '/');
            $row['image_path']   = $relNorm;       // normalized relative
            $row['image_url_full'] = $baseUrl . $relNorm;
        } else {
            $row['image_path']     = '';
            $row['image_url_full'] = null;
        }

        $data[] = [
            "id"            => (int)$row['id'],
            "item_name"     => $row['item_name'],
            "description"   => $row['description'],
            "location_found"=> $row['location_found'],
            "date_found"    => $row['date_found'],
            "item_status"   => $itemStatus,               // normalized
            "claim_status"  => $row['claim_status'],      // latest claim status or 'none'
            "created_at"    => $row['created_at'],
            "image_path"    => $row['image_path'],
            "image_url_full"=> $row['image_url_full']
        ];
    }
}

$conn->close();

/* ---------- Output ---------- */
if ($format === 'json') {
    header('Content-Type: application/json; charset=utf-8');
    echo json_encode(["status" => "success", "data" => $data], JSON_UNESCAPED_SLASHES);
    exit;
}

/* Legacy pipe output (consumed by BrowseFoundItemsPage) */
header('Content-Type: text/plain; charset=utf-8');
foreach ($data as $row) {
    echo $row['id'] . '|'
       . $row['item_name'] . '|'
       . $row['description'] . '|'
       . $row['location_found'] . '|'
       . $row['date_found'] . '|'
       . $row['item_status'] . '|'
       . $row['created_at'] . '|'
       . ($row['image_path'] ?? '') . '|'
       . $row['claim_status']
       . "\n";
}
