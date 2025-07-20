package project_lostfound;

public class User {
    private int id;
    private String name;
    private String email;
    private String role;
    private String matric;
    private String faculty;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getMatric() { return matric; }
    public void setMatric(String matric) { this.matric = matric; }

    public String getFaculty() { return faculty; }
    public void setFaculty(String faculty) { this.faculty = faculty; }
}
