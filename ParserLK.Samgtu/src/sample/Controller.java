package sample;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import junit.runner.Version;
import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;

import org.apache.http.client.methods.HttpPost;

import java.io.*;
import java.net.URL;
import java.util.*;


public class Controller implements Runnable {

    @FXML
    private ResourceBundle resources;

    @FXML
    private URL location;

    @FXML
    private Button btFile;

    @FXML
    private TableView<Profile> viewTable;

    @FXML
    private Label lbFileName;

    @FXML
    private Button btSignUp;

    @FXML
    private Label lbStatus;

    public final String VOSTANOVLENIE_PASSWORD = "Вы уже зарегистрированы в системе." +
            " Повторная регистрация не допускается! " +
            "Можете попробовать восстановить пароль, либо обратиться";

    public final String DONT_EXISTS = "По введенным Вами данным не найдено ни одного " +
            "совпадения в базе данных университета. Проверьте, правильно ли заполнены все поля,";
    String filePath;
    File fileObject;
    List<Profile> listProfile = new ArrayList<>();
    Profile newProfile;
    ObservableList<Profile> list = null;

    class UpdateTable extends Thread {
        @Override
        public void run() {
            viewTable.refresh();
        }
    }

    @FXML
    void signUpEvent(MouseEvent event) throws IOException, InterruptedException {
        if (list != null) {
            ObservableList<Profile> listUpdate = FXCollections.observableArrayList(listProfile);
            lbStatus.setText("Идет регистрация");
            for (Profile profile : list) {
//            Profile profile = list.get(0);

                try (CloseableHttpClient httpclient = HttpClients.createDefault()) {

                    List<NameValuePair> form = new ArrayList<>();
                    /************************************ LOGIN ******************************************/

//            form.add(new BasicNameValuePair("LoginForm[username]", "borisenko.gi"));
//            form.add(new BasicNameValuePair("LoginForm[password]", "121233"));
//            form.add(new BasicNameValuePair("LoginForm[rememberMe]", "0"));
//            form.add(new BasicNameValuePair("LoginForm[rememberMe]", "1"));

                    /************************************************************************************/

                    form.add(new BasicNameValuePair("SignupFormNew[email]", profile.getEmail()));
                    form.add(new BasicNameValuePair("SignupFormNew[Cellular]", profile.getCellular()));
                    form.add(new BasicNameValuePair("SignupFormNew[LastName]", profile.getLastName()));
                    form.add(new BasicNameValuePair("SignupFormNew[FirstName]", profile.getFirstName()));
                    form.add(new BasicNameValuePair("SignupFormNew[Gender]", "0"));
                    form.add(new BasicNameValuePair("SignupFormNew[Gender]", "0"));
                    form.add(new BasicNameValuePair("SignupFormNew[MiddleName]", profile.getMiddleName()));
                    form.add(new BasicNameValuePair("SignupFormNew[BirthDate]", profile.getBirthDate()));
                    form.add(new BasicNameValuePair("SignupFormNew[DocumentTypeID]", "1"));
                    form.add(new BasicNameValuePair("SignupFormNew[DocumentNumber]", profile.getDocumentNumber()));
                    form.add(new BasicNameValuePair("SignupFormNew[Agreement]", "0"));
                    form.add(new BasicNameValuePair("SignupFormNew[Agreement]", "1"));


                    UrlEncodedFormEntity entity = new UrlEncodedFormEntity(form, Consts.UTF_8);

                    HttpPost httpPost = new HttpPost("https://lk.samgtu.ru/site/signup");
                    httpPost.setEntity(entity);
                    System.out.println("Executing request " + httpPost.getRequestLine());

                    // Create a custom response handler
                    ResponseHandler<String> responseHandler = response -> {
                        int status = response.getStatusLine().getStatusCode();
                        if (status >= 200 && status < 300) {
                            HttpEntity responseEntity = response.getEntity();
                            return responseEntity != null ? EntityUtils.toString(responseEntity) : null;
                        } else {
                            throw new ClientProtocolException("Unexpected response status: " + status);
                        }
                    };
                    String responseBody = httpclient.execute(httpPost, responseHandler);
                    System.out.println("----------------------------------------");
                    //    System.out.println(responseBody);

                    if (responseBody.lastIndexOf(VOSTANOVLENIE_PASSWORD) != -1) {
                        System.out.println("Такой пользователь уже зарегистрирован");
                        profile.setStatus("Существует");

                    } else if (responseBody.lastIndexOf(DONT_EXISTS) != -1) {
                        System.out.println("Не найден ни в одной базе");
                        profile.setStatus("Не существует");

                    } else {
                        System.out.println("Может быть зарегистрирован");
                        profile.setStatus("Не зарегистрирован");

                    }
                    UpdateTable updateTable = new UpdateTable();
                    updateTable.start();
                    updateTable.join();
                } catch (Exception e) {
                    System.out.println("упс...");
                }
                viewTable.refresh();

            }
        } else {
            lbStatus.setText("Не выбран файл");
        }
    }

    //Парсинг xls
    public void parserExcelXLS(HSSFSheet sheet) {
        Iterator<Row> rowIterator = sheet.iterator();
        int row_col = 0, cell_col = 0;
        Cell cell;
        Row row;

        while (rowIterator.hasNext()) {

            row = rowIterator.next();
            if (row_col == 0) {
                row_col++;
                continue;
            }
            try {
                Iterator<Cell> cellIterator = row.cellIterator();
                newProfile = new Profile();

                for (cell_col = 0; cell_col < 20; cell_col++) {
                    switch (cell_col) {
                        case 0:
                            String pars = cellIterator.next().getStringCellValue();

                            List<String> listFIO = new ArrayList<>();
                            for (String retval : pars.split(" ")) {
                                listFIO.add(retval);
                            }
                            newProfile.setLastName(listFIO.get(0));
                            listFIO.remove(0);
                            String middlename = listFIO.get(listFIO.size() - 1);
                            newProfile.setMiddleName(middlename);
                            int last = middlename.length() - 1;
                            char ch = middlename.charAt(last);
                            if (ch == 'а') {
                                newProfile.setGender("Женщина");
                            } else {
                                newProfile.setGender("Мужчина");
                            }
                            listFIO.remove(listFIO.get(listFIO.size() - 1));
                                            /*String middleName = "";
                                            for (String e : listFIO
                                            ) {
                                                middleName += listFIO + " ";
                                            }
                                            middleName.trim();
                                            newProfile.setMiddleName(middleName);
                    */

                            newProfile.setFirstName(listFIO.get(0));
                            break;
                        case 1:
                            newProfile.setBirthDate(cellIterator.next().getStringCellValue());
                            break;

                        case 6:
                            newProfile.setDocumentNumber(cellIterator.next().getNumericCellValue());
                            break;

                        case 14:
                            newProfile.setEmail(cellIterator.next().getStringCellValue());
                            break;

                        case 18:
                            newProfile.setCellular(cellIterator.next().getStringCellValue());
                            break;
                        case 19:
                            newProfile.setAgreement("1");
                            break;
                        default:
                            cell = cellIterator.next();
                            break;
                    }
                }
                listProfile.add(newProfile);
            } catch (Exception ex) {
                System.out.println("Невозможно считать строку");
            }
        }
    }

    //Парсинг xlsx
    public void parserExcelXLS(XSSFSheet sheet) {
        Iterator<Row> rowIterator = sheet.iterator();
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

                for (cell_col = 0; cell_col < 20; cell_col++) {
                    switch (cell_col) {
                        case 0:
                            newProfile.setId(id);
                            String pars = cellIterator.next().getStringCellValue();

                            List<String> listFIO = new ArrayList<>();
                            for (String retval : pars.split(" ")) {
                                listFIO.add(retval);
                            }
                            newProfile.setLastName(listFIO.get(0));
                            listFIO.remove(0);
                            String middlename = listFIO.get(listFIO.size() - 1);
                            newProfile.setMiddleName(middlename);
                            int last = middlename.length() - 1;
                            char ch = middlename.charAt(last);
                            if (ch == 'а') {
                                newProfile.setGender("Женщина");
                            } else {
                                newProfile.setGender("Мужчина");
                            }
                            listFIO.remove(listFIO.get(listFIO.size() - 1));
                                            /*String middleName = "";
                                            for (String e : listFIO
                                            ) {
                                                middleName += listFIO + " ";
                                            }
                                            middleName.trim();
                                            newProfile.setMiddleName(middleName);
                    */

                            newProfile.setFirstName(listFIO.get(0));
                            break;
                        case 1:
                            cell = cellIterator.next();
                            if (cell.getCellType() != CellType.NUMERIC) {
                                newProfile.setBirthDate(cell.getStringCellValue());

                            } else {
                                newProfile.setBirthDateNum(cell.getNumericCellValue());
                            }

                            break;

                        case 6:
                            cell = cellIterator.next();
                            if (cell.getCellType() != CellType.NUMERIC) {
                                newProfile.setDocumentNumberStr(cell.getStringCellValue());
                            } else {
                                newProfile.setDocumentNumber(cell.getNumericCellValue());
                            }
                            break;

                        case 14:
                            newProfile.setEmail(cellIterator.next().getStringCellValue());
                            break;

                        case 18:
                            cell = cellIterator.next();
                            if (cell.getCellType() != CellType.NUMERIC) {
                                newProfile.setCellular(cell.getStringCellValue());
                            } else {
                                newProfile.setCellularNum(cell.getNumericCellValue());

                            }

                            break;
                        case 19:
                            newProfile.setAgreement("1");
                            break;
                        default:
                            cell = cellIterator.next();
                            break;
                    }
                }
                listProfile.add(newProfile);
            } catch (Exception ex) {
                System.out.println("Невозможно считать строку");
            }
        }
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
        FileChooser.ExtensionFilter xlsxfilter = new FileChooser.ExtensionFilter("xlsx files(*.xlsx)", "*.xlsx");
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

    // Считывание Excel файла (2 формата)
    public void excelRead() {

        listProfile.clear();
        try {
            FileInputStream inputStream = new FileInputStream(fileObject);
            String fileExtension = getFileExtension(fileObject.getName());
            if (fileExtension.equals(".xls")) {
                HSSFWorkbook workbook = new HSSFWorkbook(inputStream);
                HSSFSheet sheet = workbook.getSheetAt(0);
                parserExcelXLS(sheet);
            } else if (fileExtension.equals(".xlsx")) {
                XSSFWorkbook workbook = new XSSFWorkbook(inputStream);
                XSSFSheet sheet = workbook.getSheetAt(0);
                parserExcelXLS(sheet);
            }
            setViewTable(true);
        } catch (Exception e) {
            System.out.println("Невозможно открыть файл!");
        }

    }

    //Получение формата файла
    private static String getFileExtension(String mystr) {
        int index = mystr.indexOf('.');
        return index == -1 ? null : mystr.substring(index);
    }

    @FXML
    void initialize() {
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
        sexCol.setCellValueFactory(new PropertyValueFactory<>("birthDate"));
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
        statusCol.setMinWidth(100);
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

        viewTable.getColumns().addAll(idCol, surnameCol, nameCol, patronymicCol, dateCol, sexCol, emailCol, studentNumberCol, cellularCol, statusCol);

    }

    @Override
    public void run() {
        viewTable.refresh();
    }
}