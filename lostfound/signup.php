<?php
header("Content-Type: application/json");
$conn = new mysqli("localhost", "root", "", "lost_and_found");

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $data = json_decode(file_get_contents("php://input"));

    $email = $conn->real_escape_string($data->email);
    $password = $conn->real_escape_string($data->password);

    // Check if email already exists
    $check = $conn->query("SELECT * FROM user WHERE email = '$email'");
    if ($check->num_rows > 0) {
        echo json_encode(["status" => "error", "message" => "Email already registered"]);
    } else {
        // Insert new user
        $hashed = password_hash($password, PASSWORD_DEFAULT);
        $conn->query("INSERT INTO user (email, password) VALUES ('$email', '$hashed')");
        echo json_encode(["status" => "success", "message" => "User registered successfully"]);
    }
}
$conn->close();
?>
