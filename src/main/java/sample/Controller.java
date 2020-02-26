package sample;


import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class Controller {

    @FXML
    private TableView<Profile> viewTable;

    @FXML
    private Label lbFileName;

    @FXML
    private Label lbStatus;


    public final String VOSTANOVLENIE_PASSWORD = "Вы уже зарегистрированы в системе. Повторная регистрация не допускается! Можете попробовать восстановить пароль, либо обратиться в службу технической поддержки. Ваш адрес прошлой регистрации";

    public final String DONT_EXISTS = "По введенным Вами данным не найдено ни одного " +
            "совпадения в базе данных университета. Проверьте, правильно ли заполнены все поля,";
    public final String USERNSME = "<select id=\"signupformnew-username\" class=\"form-control\" name=\"SignupFormNew[username]\">\n<option value=";

    public final String EXIST_EMAIL = "На указанный Вами E-mail уже существуе";
    String filePath;
    File fileObject;
    List<Profile> listProfile = new ArrayList<>();
    Profile newProfile;
    ObservableList<Profile> list = null;
    HSSFWorkbook workbook = null;
    XSSFWorkbook workbookXSSF = null;
    XSSFSheet sheetXSSF = null;
    HSSFSheet sheet = null;
    String fileExtension;

    Boolean CHANGE_STATUS = false;
    Boolean REGISTRATION = true;
    Boolean FIRST_REQUEST = false;
    Boolean SECOND_REQUEST = true;
    XSSFCellStyle styleXSSF = null;

    public String request(Profile profile, boolean full) {
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            List<NameValuePair> form = new ArrayList<NameValuePair>();

            form.add(new BasicNameValuePair("SignupFormNew[email]", profile.getEmail()));
            form.add(new BasicNameValuePair("SignupFormNew[Cellular]", profile.getCellular()));
            form.add(new BasicNameValuePair("SignupFormNew[LastName]", profile.getLastName()));
            form.add(new BasicNameValuePair("SignupFormNew[FirstName]", profile.getFirstName()));
            form.add(new BasicNameValuePair("SignupFormNew[Gender]", ""));
            if (profile.getGender() != "*1") {
                form.add(new BasicNameValuePair("SignupFormNew[Gender]", profile.getGender()));
            } else {
                form.add(new BasicNameValuePair("SignupFormNew[Gender]", "1"));
            }
            form.add(new BasicNameValuePair("SignupFormNew[MiddleName]", profile.getMiddleName()));
            form.add(new BasicNameValuePair("SignupFormNew[BirthDate]", profile.getBirthDate()));
            form.add(new BasicNameValuePair("SignupFormNew[DocumentTypeID]", "1"));
            form.add(new BasicNameValuePair("SignupFormNew[DocumentNumber]", profile.getDocumentNumber()));
            form.add(new BasicNameValuePair("SignupFormNew[Agreement]", "0"));
            form.add(new BasicNameValuePair("SignupFormNew[Agreement]", "1"));
            if (full) {
                form.add(new BasicNameValuePair("SignupFormNew[t]", "1"));
                form.add(new BasicNameValuePair("SignupFormNew[username]", profile.getUsername()));
                form.add(new BasicNameValuePair("SignupFormNew[password]", profile.getPassword()));
                form.add(new BasicNameValuePair("SignupFormNew[confirmpassword]", profile.getPassword()));
            }
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(form, Consts.UTF_8);

            HttpPost httpPost = new HttpPost("https://lk.samgtu.ru/site/signup");
            httpPost.setEntity(entity);
            System.out.println("Executing request " + httpPost.getRequestLine());

            // Create a custom response handler
            ResponseHandler<String> responseHandler = response -> {
                int status = response.getStatusLine().getStatusCode();
                if (status >= 200 && status < 303) {
                    HttpEntity responseEntity = response.getEntity();
                    return responseEntity != null ? EntityUtils.toString(responseEntity) : null;
                } else {
                    throw new ClientProtocolException("Unexpected response status: " + status);
                }
            };
            String responseBody = httpclient.execute(httpPost, responseHandler);
            return responseBody;
        } catch (Exception e) {
            profile.setStatus("Ошибка");
        }
        return null;
    }

    public boolean checkStatusByResponse(Profile profile, String response) {
        if (response.lastIndexOf(VOSTANOVLENIE_PASSWORD) != -1) {
            int indexEnd = response.lastIndexOf(VOSTANOVLENIE_PASSWORD);
            for (int i = 3; response.charAt(indexEnd + VOSTANOVLENIE_PASSWORD.length() + i) != '\n'; i++) {
                profile.setStatus(response.substring(indexEnd, indexEnd + VOSTANOVLENIE_PASSWORD.length() + i + 1));
            }
            writeExcel(profile, CHANGE_STATUS);
            return true;
        } else if (response.lastIndexOf(DONT_EXISTS) != -1) {
            int indexEnd = response.lastIndexOf(DONT_EXISTS);
            for (int i = 3; response.charAt(indexEnd + DONT_EXISTS.length() + i) != '\n'; i++) {
                profile.setStatus(response.substring(indexEnd, indexEnd + DONT_EXISTS.length() + i + 1));
            }
            writeExcel(profile, CHANGE_STATUS);
            return true;
        } else if (response.lastIndexOf(EXIST_EMAIL) != -1) {
            int indexEnd = response.lastIndexOf(EXIST_EMAIL);
            for (int i = 3; response.charAt(indexEnd + EXIST_EMAIL.length() + i) != '\n'; i++) {
                profile.setStatus(response.substring(indexEnd, indexEnd + EXIST_EMAIL.length() + i + 1));
            }
            writeExcel(profile, CHANGE_STATUS);
            return true;
        }
        return false;
    }

    public void errorMessage() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Ошибка");
        alert.setHeaderText("Файл уже открыт!");
        alert.setContentText(filePath);
        alert.showAndWait().ifPresent(rs -> {
            if (rs == ButtonType.OK) {
                return;
            }
        });
    }

    public void clearStatusForChoose(){
        if (list != null) {
            for (Profile profile: list
                 ) {
                if (profile.isCheckReg() && profile.getStatus()!=null) {
                    profile.setStatus("*");
                }
            }
        }
        viewTable.refresh();
    }

    @FXML
    void checkSignUpEvent(MouseEvent event) {
        if (list != null) {
            FileOutputStream out;
            try {
                out = new FileOutputStream(fileObject);
                //    save(out);
                out.close();
            } catch (Exception e) {
                errorMessage();
                return;
            }
            lbStatus.setText("Идет проверка");


            new Thread(() -> {
                clearStatusForChoose();
                try {
                    boolean writed = false;
                    for (Profile profile : list) {
                        if (profile.isCheckReg() == true) {
                            //Отправка запроса и получение ответа
                            String responseBody = request(profile, FIRST_REQUEST);
                            System.out.println("----------------------------------------");

                            //Проверка ответа на различные статусы и запись статусов в excel
                            if (!checkStatusByResponse(profile, responseBody)) {
                                System.out.println("Может быть зарегистрирован");
                                profile.setStatus(null);
                                writeExcel(profile, CHANGE_STATUS);
                            }
                            writed = true;
                            viewTable.refresh();
                        }
                    }
                    if (!writed) {
                        save();
                    }
                } catch (Exception e) {

                }
            }).start();



        } else {
            lbStatus.setText("Не выбран файл");
        }
        lbStatus.setText("Проверка завершена");
    }


    @FXML
    void signUpEvent(MouseEvent event) {
        if (list != null) {
            FileOutputStream out;
            try {
                out = new FileOutputStream(fileObject);
                out.close();
            } catch (Exception e) {
                errorMessage();
                return;
            }
            lbStatus.setText("Идет регистрация");
            new Thread(() -> {
                clearStatusForChoose();
                try {
                    boolean writed = false;
                    for (Profile profile : list) {
                        if (profile.isCheckReg() == true) {

                            String responseBody = request(profile, FIRST_REQUEST);
                            System.out.println("----------------------------------------");

                            //Проверка ответа на различные статусы и запись статусов в excel
                            if (!checkStatusByResponse(profile, responseBody)) {
                                //Парсинг логина из HTML ответа
                                int indexEnd = responseBody.lastIndexOf(USERNSME);
                                for (int i = 3; responseBody.charAt(indexEnd + USERNSME.length() + i) != '>'; i++) {
                                    profile.setUsername(responseBody.substring(indexEnd + USERNSME.length() + 1, indexEnd + USERNSME.length() + i));
                                }
                                //Генерация пароля
                                String password = generatePassword();
                                profile.setPassword(password);
                                //Запуск второго запроса(логин и пароль)
                                request(profile, SECOND_REQUEST);
                                if (profile.getStatus() == null)
                                    profile.setStatus("Зарегистрирован");
                                writeExcel(profile, REGISTRATION);

                            }
                            writed = true;
                            viewTable.refresh();
                        }
                    }
                    if (!writed) {
                        save();
                    }

                } catch (Exception e) {

                }
            }).start();


        } else {
            lbStatus.setText("Не выбран файл");
        }
        lbStatus.setText("Регистрация завершена");
    }

    public void save() {
        try {
            FileOutputStream out = new FileOutputStream(filePath);
            if (workbookXSSF != null) {
                workbookXSSF.write(out);
            } else if (workbook != null) {
                workbook.write(out);
            }
        } catch (Exception e) {

        }
    }

    //Генерация пароля из чисел, строчных и прописных сиволов, минимум с 2 числами
    public String generatePassword() {
        boolean codeTrue = false;
        String password;
        do {
            password = RandomStringUtils.random(8, "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789");
            byte col = 0;
            boolean isUpper = false;
            for (int i = 0; i < password.length(); i++) {

                if (Character.isDigit(password.charAt(i))) {
                    col++;
                } else if (Character.isUpperCase(password.charAt(i))) {
                    isUpper = true;
                }

            }
            if (col > 1 && isUpper) {
                codeTrue = true;
            }

        } while (!codeTrue);
        return password;
    }

    public void parserExcel(Iterator<Row> rowIterator) {
        int row_col = 0, cell_col = 0;
        Cell cell;
        Row row;

        int id = -1;
        while (rowIterator.hasNext()) {
            id++;
            row = rowIterator.next();
            if (row_col == 0) {
                row_col++;
                continue;
            }


            try {
                Iterator<Cell> cellIterator = row.cellIterator();
                newProfile = new Profile();

                for (cell_col = 0; cell_col < 23; cell_col++) {
                    switch (cell_col) {
                        case 0:
                            newProfile.setId(id);
                            String pars = cellIterator.next().getStringCellValue();

                            List<String> listFIO = new ArrayList<>();
                            for (String retval : pars.split(" ")) {
                                listFIO.add(retval);
                            }
                            if (listFIO.size() == 3) {
                                newProfile.setLastName(listFIO.get(0));
                                newProfile.setFirstName(listFIO.get(1));
                                newProfile.setMiddleName(listFIO.get(2));
                                int last = newProfile.getMiddleName().length() - 1;
                                char ch = newProfile.getMiddleName().charAt(last);
                                if (ch == 'а') {
                                    newProfile.setGender("Женщина");
                                } else {
                                    newProfile.setGender("Мужчина");
                                }
                            } else {
                                newProfile.setLastName(listFIO.get(0));

                                for (int i = 1; i < listFIO.size(); i++) {
                                    if (newProfile.getFirstName() != null) {
                                        newProfile.setFirstName(newProfile.getFirstName() + " " + listFIO.get(i));
                                    } else {
                                        newProfile.setFirstName(listFIO.get(i));
                                    }
                                }
                                newProfile.setGender("*Мужчина");
                                newProfile.setFirstName(newProfile.getFirstName().trim());
                                newProfile.setMiddleName("");
                            }
                            break;
                        case 1:
                            cell = cellIterator.next();

                            if (cell.getCellType() == CellType.STRING) {
                                newProfile.setBirthDate(cell.getStringCellValue());

                            } else {
                                newProfile.setBirthDateNum(cell.getDateCellValue());
                            }

                            break;

                        case 6:
                            cell = cellIterator.next();
                            cell.setCellType(CellType.STRING);
                            if (cell.getCellType() != CellType.NUMERIC) {
                                newProfile.setDocumentNumberStr(cell.getStringCellValue());
                            } else {
                                newProfile.setDocumentNumber(cell.getNumericCellValue());
                            }
                            break;

                        case 14:
                            cell = cellIterator.next();
                            cell.setCellType(CellType.STRING);
                            newProfile.setEmail(cell.getStringCellValue());
                            break;

                        case 18:
                            cell = cellIterator.next();
                            cell.setCellType(CellType.STRING);
                            if (cell.getCellType() != CellType.NUMERIC) {
                                newProfile.setCellular(cell.getStringCellValue());
                            } else {
                                newProfile.setCellularNum(cell.getNumericCellValue());

                            }

                            break;
                        case 19:
                            newProfile.setAgreement("1");
                            break;
                        case 22:
                            cell = cellIterator.next();
                            cell = cellIterator.next();
                            newProfile.setStatus(cell.getStringCellValue());
                        default:
                            try {
                                cell = cellIterator.next();
                            } catch (Exception e) {

                            }
                            break;
                    }
                }
                listProfile.add(newProfile);
            } catch (Exception ex) {
                System.out.println("Невозможно считать строку");
            }
        }
    }

    //Парсинг xls
    public void parserExcelXLS(HSSFSheet sheet) {
        Iterator<Row> rowIterator = sheet.iterator();
        parserExcel(rowIterator);
    }

    //Парсинг xlsx
    public void parserExcelXLS(XSSFSheet sheet) {
        Iterator<Row> rowIterator = sheet.iterator();
        parserExcel(rowIterator);
    }

    public void setViewTable(boolean first) {
        lbStatus.setText("Создание таблицы...");
        viewTable.getItems().clear();
        list = null;
        list = FXCollections.observableArrayList(listProfile);
        viewTable.setItems(list);
        lbStatus.setText("");
    }

    //Выбор файла
    @FXML
    void clickBtFileEvent(MouseEvent event) {
        Node source = (Node) event.getSource();
        Stage PrimaryStage = (Stage) source.getScene().getWindow();
        FileChooser fileChooser = new FileChooser();
        FileChooser.ExtensionFilter xlsxfilter = new FileChooser.ExtensionFilter("xlsx files(*.xlsx)", "*.xlsx", "*.xls");
        fileChooser.getExtensionFilters().add(xlsxfilter);
        fileChooser.getExtensionFilters().addAll(xlsxfilter);
        fileChooser.setTitle("Выбор файла");
        fileObject = fileChooser.showOpenDialog(PrimaryStage);
        if (fileObject != null) {
            filePath = fileObject.getPath();
            lbFileName.setText(fileObject.getName());
        } else {
            lbFileName.setText("");
        }
        lbStatus.setText("Считывание Excel файла");
        excelRead();
    }

    public void writeExcel(Profile profile, boolean reg) {
        try {
            FileOutputStream out = new FileOutputStream(filePath);
            if (fileExtension.equals(".xls")) {
                sheet.getRow(0).createCell(22, CellType.STRING).setCellValue("Статус");
                sheet.getRow(profile.getId()).createCell(22, CellType.STRING).setCellValue(profile.getStatus());
                if (reg) {
                    sheet.getRow(profile.getId()).getCell(11).setCellValue(profile.getUsername());
                    sheet.getRow(profile.getId()).getCell(12).setCellValue(profile.getPassword());
                }
                workbook.write(out);
            } else if (fileExtension.equals(".xlsx")) {

                sheetXSSF.getRow(0).createCell(22, CellType.STRING).setCellValue("Статус");

                sheetXSSF.getRow(profile.getId()).createCell(22, CellType.STRING).setCellValue(profile.getStatus());
                if (reg) {
                    sheetXSSF.getRow(profile.getId()).getCell(11).setCellValue(profile.getUsername());
                    sheetXSSF.getRow(profile.getId()).getCell(12).setCellValue(profile.getPassword());
                }
                workbookXSSF.write(out);
            }
            out.close();
        } catch (Exception e) {
            System.out.println("Невозможно записть изменения в файл, возможно файл уже открыт.");
        }
    }

    // Считывание Excel файла (2 формата)
    public void excelRead() {
        workbook = null;
        workbookXSSF = null;
        sheet = null;
        sheetXSSF = null;

        listProfile.clear();
        try {
            FileInputStream inputStream = new FileInputStream(fileObject);
            fileExtension = getFileExtension(fileObject.getName(), false);
            if (fileExtension.equals(".xls")) {
                workbook = new HSSFWorkbook(inputStream);
                sheet = workbook.getSheetAt(0);
                parserExcelXLS(sheet);
            } else if (fileExtension.equals(".xlsx")) {
                workbookXSSF = new XSSFWorkbook(inputStream);
                sheetXSSF = workbookXSSF.getSheetAt(0);
                styleXSSF = workbookXSSF.createCellStyle();
                parserExcelXLS(sheetXSSF);
            }
            setViewTable(true);
        } catch (Exception e) {
            System.out.println("Невозможно открыть файл!");
        }

    }

    //Получение формата файла
    private static String getFileExtension(String mystr, boolean fileName) {
        int index = mystr.indexOf('.');
        if (!fileName) {
            return index == -1 ? null : mystr.substring(index);
        } else {
            return index == -1 ? null : mystr.substring(0, index);
        }
    }

    @FXML
    void btTakeDrop(MouseEvent event) {
        boolean isOne = false;
        if (!listProfile.isEmpty())
            for (Profile pr : listProfile
            ) {
                if (!pr.isCheckReg() == true) {
                    pr.setCheckReg(true);
                    isOne = true;
                }
            }
        if (isOne == false) {
            for (Profile pr : listProfile
            ) {
                pr.setCheckReg(false);

            }
        }
        viewTable.refresh();
    }

    @FXML
    void initialize() {

        TableColumn<Profile, Boolean> checkRegCol
                = new TableColumn<Profile, Boolean>("Выбрать");

        TableColumn<Profile, String> idCol
                = new TableColumn<Profile, String>("ID");

        TableColumn<Profile, String> surnameCol
                = new TableColumn<Profile, String>("Фамилия");

        TableColumn<Profile, String> nameCol
                = new TableColumn<Profile, String>("Имя");

        TableColumn<Profile, String> patronymicCol
                = new TableColumn<Profile, String>("Отчество");

        TableColumn<Profile, String> dateCol
                = new TableColumn<Profile, String>("Дата рождения");

        TableColumn<Profile, String> sexCol
                = new TableColumn<Profile, String>("Пол");

        TableColumn<Profile, String> studentNumberCol
                = new TableColumn<Profile, String>("Номер зачетки");

        TableColumn<Profile, String> emailCol
                = new TableColumn<Profile, String>("Email");

        TableColumn<Profile, String> cellularCol
                = new TableColumn<Profile, String>("Номер телефона");

        TableColumn<Profile, String> statusCol
                = new TableColumn<Profile, String>("Статус");

        viewTable.setEditable(true);

        checkRegCol.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<Profile, Boolean>, ObservableValue<Boolean>>() {

            @Override
            public ObservableValue<Boolean> call(TableColumn.CellDataFeatures<Profile, Boolean> param) {
                Profile profile = param.getValue();

                SimpleBooleanProperty booleanProp = new SimpleBooleanProperty(profile.isCheckReg());

                booleanProp.addListener(new ChangeListener<Boolean>() {

                    @Override
                    public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue,
                                        Boolean newValue) {
                        profile.setCheckReg(newValue);
                    }
                });
                return booleanProp;
            }
        });

        checkRegCol.setCellFactory(new Callback<TableColumn<Profile, Boolean>, //
                TableCell<Profile, Boolean>>() {
            @Override
            public TableCell<Profile, Boolean> call(TableColumn<Profile, Boolean> p) {
                CheckBoxTableCell<Profile, Boolean> cell = new CheckBoxTableCell<Profile, Boolean>();
                cell.setAlignment(Pos.CENTER);
                return cell;
            }
        });

        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        surnameCol.setMaxWidth(20);

        surnameCol.setCellValueFactory(new PropertyValueFactory<>("lastName"));
        surnameCol.setCellFactory(TextFieldTableCell.<Profile>forTableColumn());
        surnameCol.setMinWidth(140);
        surnameCol.setOnEditCommit((TableColumn.CellEditEvent<Profile, String> event) -> {
            TablePosition<Profile, String> pos = event.getTablePosition();

            String newLastName = event.getNewValue();

            int row = pos.getRow();
            Profile person = event.getTableView().getItems().get(row);

            person.setLastName(newLastName);
        });

        nameCol.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        nameCol.setCellFactory(TextFieldTableCell.<Profile>forTableColumn());
        nameCol.setMinWidth(140);
        nameCol.setOnEditCommit((TableColumn.CellEditEvent<Profile, String> event) -> {
            TablePosition<Profile, String> pos = event.getTablePosition();

            String newFirstName = event.getNewValue();

            int row = pos.getRow();
            Profile person = event.getTableView().getItems().get(row);

            person.setFirstName(newFirstName);
        });

        patronymicCol.setCellValueFactory(new PropertyValueFactory<>("middleName"));
        patronymicCol.setCellFactory(TextFieldTableCell.<Profile>forTableColumn());
        patronymicCol.setMinWidth(140);
        patronymicCol.setOnEditCommit((TableColumn.CellEditEvent<Profile, String> event) -> {
            TablePosition<Profile, String> pos = event.getTablePosition();

            String newMiddleName = event.getNewValue();

            int row = pos.getRow();
            Profile person = event.getTableView().getItems().get(row);

            person.setMiddleName(newMiddleName);
        });

        dateCol.setCellValueFactory(new PropertyValueFactory<>("birthDate"));
        dateCol.setCellFactory(TextFieldTableCell.<Profile>forTableColumn());
        dateCol.setMinWidth(80);
        dateCol.setOnEditCommit((TableColumn.CellEditEvent<Profile, String> event) -> {
            TablePosition<Profile, String> pos = event.getTablePosition();

            String newData = event.getNewValue();

            int row = pos.getRow();
            Profile person = event.getTableView().getItems().get(row);

            person.setBirthDate(newData);
        });

        //TODO: Combobox
        sexCol.setCellValueFactory(new PropertyValueFactory<>("gender"));
        sexCol.setCellFactory(TextFieldTableCell.<Profile>forTableColumn());
        sexCol.setMinWidth(80);
        sexCol.setOnEditCommit((TableColumn.CellEditEvent<Profile, String> event) -> {
            TablePosition<Profile, String> pos = event.getTablePosition();

            String newGender = event.getNewValue();

            int row = pos.getRow();
            Profile person = event.getTableView().getItems().get(row);

            person.setGender(newGender);
        });

        emailCol.setCellValueFactory(new PropertyValueFactory<>("email"));
        emailCol.setCellFactory(TextFieldTableCell.<Profile>forTableColumn());
        emailCol.setMinWidth(170);
        emailCol.setOnEditCommit((TableColumn.CellEditEvent<Profile, String> event) -> {
            TablePosition<Profile, String> pos = event.getTablePosition();

            String newEmail = event.getNewValue();

            int row = pos.getRow();
            Profile person = event.getTableView().getItems().get(row);

            person.setEmail(newEmail);
        });

        studentNumberCol.setCellValueFactory(new PropertyValueFactory<>("documentNumber"));
        studentNumberCol.setCellFactory(TextFieldTableCell.<Profile>forTableColumn());
        studentNumberCol.setMinWidth(80);
        studentNumberCol.setOnEditCommit((TableColumn.CellEditEvent<Profile, String> event) -> {
            TablePosition<Profile, String> pos = event.getTablePosition();

            String newStudentNumber = event.getNewValue();

            int row = pos.getRow();
            Profile person = event.getTableView().getItems().get(row);

            person.setDocumentNumberStr(newStudentNumber);
        });

        cellularCol.setCellValueFactory(new PropertyValueFactory<>("cellular"));
        cellularCol.setCellFactory(TextFieldTableCell.<Profile>forTableColumn());
        cellularCol.setMinWidth(100);
        cellularCol.setOnEditCommit((TableColumn.CellEditEvent<Profile, String> event) -> {
            TablePosition<Profile, String> pos = event.getTablePosition();

            String newCellular = event.getNewValue();

            int row = pos.getRow();
            Profile person = event.getTableView().getItems().get(row);

            person.setCellular(newCellular);
        });
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setCellFactory(TextFieldTableCell.<Profile>forTableColumn());
        statusCol.setMinWidth(1380);
        statusCol.setOnEditCommit((TableColumn.CellEditEvent<Profile, String> event) -> {
            TablePosition<Profile, String> pos = event.getTablePosition();

            String newStatus = event.getNewValue();

            int row = pos.getRow();
            Profile person = event.getTableView().getItems().get(row);

            person.setStatus(newStatus);
        });

        idCol.setSortType(TableColumn.SortType.DESCENDING);
        surnameCol.setSortType(TableColumn.SortType.DESCENDING);
        nameCol.setSortType(TableColumn.SortType.DESCENDING);
        patronymicCol.setSortType(TableColumn.SortType.DESCENDING);
        dateCol.setSortType(TableColumn.SortType.DESCENDING);
        sexCol.setSortType(TableColumn.SortType.DESCENDING);
        emailCol.setSortType(TableColumn.SortType.DESCENDING);
        studentNumberCol.setSortType(TableColumn.SortType.DESCENDING);
        cellularCol.setSortType(TableColumn.SortType.DESCENDING);
        statusCol.setSortType(TableColumn.SortType.DESCENDING);

        viewTable.getColumns().addAll(checkRegCol, idCol, surnameCol, nameCol, patronymicCol, dateCol, sexCol, emailCol, studentNumberCol, cellularCol, statusCol);


    }

    @FXML
    void btSave(MouseEvent event) {
        if (workbookXSSF != null) {
            try (FileOutputStream out = new FileOutputStream(new File(getFileExtension(filePath, true) + "_NEW.xlsx"))) {
                workbookXSSF.write(out);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (workbook != null) {
            try (FileOutputStream out = new FileOutputStream(new File(getFileExtension(filePath, true) + "_NEW.xls"))) {
                workbook.write(out);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public TableView<Profile> getViewTable() {
        return viewTable;
    }
}