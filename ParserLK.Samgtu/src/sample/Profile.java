package sample;

public class Profile {
    private int id;
    private String email;
    private String cellular;
    private String lastName;
    private String firstName;
    private String gender;
    private String middleName;
    private String birthDate;
    private String documentNumber;
    private String agreement;
  // private Status status;
private String status;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCellular() {
        return cellular;
    }

    public void setCellularNum(double cellular) {
        this.cellular = Double.toString(cellular);
    }

    public void setCellular(String cellular) {
        this.cellular = cellular;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(String birthDate) {
        this.birthDate = birthDate;
    }

    public void setBirthDateNum(double birthDate) {
        this.birthDate = Double.toString(birthDate);
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public void setDocumentNumber(double documentNumber) {
        this.documentNumber = Double.toString(documentNumber);
    }

    public void setDocumentNumberStr(String documentNumber) {
        this.documentNumber = documentNumber;
    }

    public String getAgreement() {
        return agreement;
    }

    public void setAgreement(String agreement) {
        this.agreement = agreement;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
