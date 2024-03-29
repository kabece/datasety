package application;

import application.implementations.parser.XmlParser;
import application.interfaces.analyzer.Analyzer;
import com.codesnippets4all.json.parsers.JSONParser;
import com.codesnippets4all.json.parsers.JsonParserFactory;
import enums.FileType;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;

/**
 * Klasa zajmująca się tworzeniem widoku danych w postaci tabeli.
 */
@SuppressWarnings("WeakerAccess")
public class DynamicTable {

	private static final Logger logger = LogManager.getLogger(DynamicTable.class.getName());

	private TableView<ObservableList<StringProperty>> table;
	private File file;
	private FileType fileType;
	private Analyzer analyzer;

	/**
	 * Podstawowy konstruktor
	 * @param table Zapełniana tabela
	 * @param file Plik z danymi
	 * @param fileType Typ (rozszerzenie) pliku
	 * @param analyzer Komponent analizujący dane w kontekście wyrażeń
	 */
	public DynamicTable(final TableView<ObservableList<StringProperty>> table, final File file, final FileType fileType,
			application.interfaces.analyzer.Analyzer analyzer) {
		this.table = table;
		this.file = file;
		this.fileType = fileType;
		this.analyzer = analyzer;
	}

	/**
	 * Metoda zajmująca się zapełnianiem tabeli danymi
	 */
	public void populateTable() {
		logger.info("Start populateTable");

		table.getItems().clear();
		table.getColumns().clear();
		table.setPlaceholder(new Label("Loading..."));
		Task<Void> task;
		switch (fileType) {
			case CSV:
				task = parseCsvContent();
				break;
			case JSON:
				task = parseJson();
				break;
			case XML:
				task = parseXML();
				break;
			default:
				task = null;
				break;
		}
		if (task != null) {
			Thread thread = new Thread(task);
			thread.setDaemon(true);
			thread.start();
		}

		logger.info("Finish populateTable");
	}

	/**
	 * Przetwarza dane z pliku CSV, następnie dodaje je do tabeli.
	 * Zapisuje je także w wewnętrznej strukturze na potrzeby późniejszej analizy.
	 *
	 * @return Zadanie (Task) parsujące CSV
	 */

	//TODO: z tego co zauwazylem, jesli sei odznaczy "hasHeader" to sie psuje :(
	private Task<Void> parseCsvContent() {
		logger.info("Start parseCsvContent");

		Task<Void> task = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				logger.info("Start call (inside parseCsvContent)");

				BufferedReader in = new BufferedReader(new FileReader(file));
				// Header line

					final String headerLine = in.readLine();
					final String[] headerValues = headerLine.split(",");


					Platform.runLater(() -> {
						analyzer.getDatasets().put(file.getName(), new HashMap<>());
						analyzer.getDataHeaders().put(file.getName(), new ArrayList<>());

						for (int column = 0; column < headerValues.length; column++) {
							table.getColumns().add(createColumn(column, headerValues[column]));
							analyzer.getDatasets().get(file.getName()).put(headerValues[column], new ArrayList<>());

							analyzer.getDataHeaders().get(file.getName()).add(headerValues[column]);
						}
						Main.dataVariables.put(file.getName(),new ArrayList<String>(Arrays.asList(headerValues)));
					});


				// Data:

				String dataLine;
				while ((dataLine = in.readLine()) != null) {
					final String[] dataValues = dataLine.split(",");
					Platform.runLater(() -> {
						// Add additional columns if necessary:
						for (int columnIndex = table.getColumns().size(); columnIndex < dataValues.length; columnIndex++) {
							table.getColumns().add(createColumn(columnIndex, ""));
						}
						// Add dataMap to table:
						ObservableList<StringProperty> data = FXCollections.observableArrayList();
						for (String value : dataValues) {
							data.add(new SimpleStringProperty(value));
						}

							for (int i = 0; i < data.size(); i++) {
								analyzer.getDatasets().get(file.getName()).get(analyzer.getDataHeaders().get(file.getName()).get(i)).add(data.get(i).getValue());
							}

						table.getItems().add(data);
					});
				}
				in.close();

				logger.info("Finish call (inside parseCsvContent)");
				return null;
			}
		};

		logger.info("Finish parseCsvContent");
		return task;
	}

	private Task<Void> parseJson() {
		logger.info("Start parseJson");


		Task<Void> task = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				logger.info("Start call (inside parseJson)");
				JsonParserFactory factory = JsonParserFactory.getInstance();
				JSONParser parser = factory.newJsonParser();

				BufferedReader in = new BufferedReader(new FileReader(file));
				String firstLine = in.readLine();
				Map singleJson = parser.parseJson(firstLine);

				final String[] headerValues =  Arrays.copyOf(singleJson.keySet().toArray(), singleJson.keySet().toArray().length, String[].class);


				Platform.runLater(() -> {
					analyzer.getDatasets().put(file.getName(), new HashMap<>());
					analyzer.getDataHeaders().put(file.getName(), new ArrayList<>());
					for (int column = 0; column < headerValues.length; column++) {
						table.getColumns().add(createColumn(column, headerValues[column]));
						analyzer.getDatasets().get(file.getName()).put(headerValues[column], new ArrayList<>());
						analyzer.getDataHeaders().get(file.getName()).add(headerValues[column]);
					}
					Main.dataVariables.put(file.getName(),new ArrayList<String>(Arrays.asList(headerValues)));
				});

				for(String line; (line = in.readLine()) != null; ) {
					final Map singleJsonLine = parser.parseJson(line);

					Platform.runLater(() -> {
						ObservableList<StringProperty> data = FXCollections.observableArrayList();
						for (String key : headerValues) {
							data.add(new SimpleStringProperty(singleJsonLine.get(key).toString()));
						}

						for (int i = 0; i < data.size(); i++) {
							analyzer.getDatasets().get(file.getName()).get(analyzer.getDataHeaders().get(file.getName()).get(i)).add(data.get(i).getValue());
						}

						table.getItems().add(data);

					});
				}
				in.close();

				return null;
			}
		};
		logger.info("Finished parseJson");
		return task;
	}

	private Task<Void> parseXML() {
		logger.info("Start parseXML");

		Task<Void> task = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				XmlParser parser = new XmlParser(file);

				Map<String,ArrayList<String>> dataset = parser.parseXml();

				analyzer.getDatasets().put(file.getName(), new HashMap<>(dataset));

				final String[] headerValues =  Arrays.copyOf(dataset.keySet().toArray(), dataset.keySet().toArray().length, String[].class);


				analyzer.getDataHeaders().put(file.getName(), new ArrayList<>());
				Platform.runLater(() -> {
					for (int column = 0; column < headerValues.length; column++) {
						table.getColumns().add(createColumn(column, headerValues[column]));
					}
					Main.dataVariables.put(file.getName(),new ArrayList<>(Arrays.asList(headerValues)));

					for(int i = 0; i < dataset.get(headerValues[0]).size(); i++) {

						ObservableList<StringProperty> data = FXCollections.observableArrayList();
						for (String key : headerValues) {
							data.add(new SimpleStringProperty(dataset.get(key).get(i).toString()));
							table.getItems().add(data);
						}
					}
				});

				return null;
			}
		};

		return task;

	}

	/**
	 * Tworzy kolumny tabeli, z odpowiednimi nagłówkami oraz dodaje fabrykę komórek tabeli
	 *
	 * @param columnIndex Indeks kolumny
	 * @param columnTitle Nazwa kolumny (nagłówek)
	 * @return Kolumna tabeli
	 */
	private TableColumn<ObservableList<StringProperty>, String> createColumn(final int columnIndex, String columnTitle) {
		TableColumn<ObservableList<StringProperty>, String> column = new TableColumn<>();
		String title;
		if (columnTitle == null || columnTitle.trim().length() == 0) {
			title = "Column " + (columnIndex + 1);
		} else {
			title = columnTitle;
		}
		column.setText(title);

		column.setCellValueFactory(cellDataFeatures -> {
			ObservableList<StringProperty> values = cellDataFeatures.getValue();
			if (columnIndex >= values.size()) {
				return new SimpleStringProperty("");
			} else {
				return cellDataFeatures.getValue().get(columnIndex);
			}
		});

		return column;
	}

}
