<?php
$host = "localhost";
$user = "root";
$password = "";
$database = "lost_and_found";

$conn = new mysqli($host, $user, $password, $database);
if ($conn->connect_error) {
    die("connection_error");
}

$action = $_GET['action'] ?? '';

if ($action == 'all_users') {
    $result = $conn->query("SELECT id, name, email, matric, faculty, role FROM user");
    while ($row = $result->fetch_assoc()) {
        // Use | as delimiter, escape if necessary
        echo $row['id'] . "|" . $row['name'] . "|" . $row['email'] . "|" .
             $row['matric'] . "|" . $row['faculty'] . "|" . $row['role'] . "\n";
    }
} elseif ($action == 'pending_items') {
    $result = $conn->query("SELECT id, type, title, status, reporter_name, date_reported FROM items WHERE status = 'pending'");
    while ($row = $result->fetch_assoc()) {
        echo $row['id'] . "|" . $row['type'] . "|" . $row['title'] . "|" .
             $row['status'] . "|" . $row['reporter_name'] . "|" . $row['date_reported'] . "\n";
    }
} elseif ($action == 'all_items') {
    $result = $conn->query("SELECT id, type, title, status, reporter_name, date_reported FROM items");
    while ($row = $result->fetch_assoc()) {
        echo $row['id'] . "|" . $row['type'] . "|" . $row['title'] . "|" .
             $row['status'] . "|" . $row['reporter_name'] . "|" . $row['date_reported'] . "\n";
    }
} else {
    echo "invalid_action";
}

$conn->close();
?>
