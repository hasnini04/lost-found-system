<?php
header("Content-Type: application/json");

// Database connection
$host = "localhost";
$dbname = "lost_and_found";
$user = "root";
$pass = "";

try {
    $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8mb4", $user, $pass);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

    // Fetch all reporters (role = reporter)
    $stmt = $pdo->prepare("SELECT id, name, matric, faculty FROM user WHERE role = 'reporter'");
    $stmt->execute();
    $reporters = $stmt->fetchAll(PDO::FETCH_ASSOC);

    echo json_encode($reporters);
} catch (PDOException $e) {
    echo json_encode(["error" => $e->getMessage()]);
}
?>
