<?php
header("Content-Type: application/json");
$conn = new mysqli("localhost", "root", "", "lost_and_found");

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $data = json_decode(file_get_contents("php://input"));
    $email = $conn->real_escape_string($data->email);
    $password = $data->password;

    $result = $conn->query("SELECT * FROM user WHERE email = '$email'");

    if ($result->num_rows === 1) {
        $user = $result->fetch_assoc();
        if (password_verify($password, $user['password'])) {
            echo json_encode(["status" => "success", "email" => $email]);
        } else {
            echo json_encode(["status" => "error", "message" => "Incorrect password"]);
        }
    } else {
        echo json_encode(["status" => "error", "message" => "User not found"]);
    }
}
$conn->close();
?>
