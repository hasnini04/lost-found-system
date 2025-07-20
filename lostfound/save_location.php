<?php
if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_POST['address'])) {
    $address = $_POST['address'];
    file_put_contents("selected_location_found.txt", $address);
    echo "Location saved successfully";
} else {
    echo "No address received";
}
?>
