package application;

import constants.AnalyzerWorkType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by kczurylo on 2016-08-10.
 * Klasa zajmująca się analizowaniem danych pod względem zadanej logiki
 */
@SuppressWarnings("WeakerAccess")
public class Analyzer {

	private Logger logger = LogManager.getLogger(Analyzer.class.getName());

	private Map<String, ArrayList<String>> dataMap;
	private List<String> dataHeaders;
	private AnalyzerWorkType analyzerWorkType;

	/**
	 * Podstawowy konstruktor
	 */
	public Analyzer() {
		dataMap = new HashMap<>();
		dataHeaders = new ArrayList<>();
	}

	/**
	 * Podstawowy analizator wyrażeń
	 *
	 * @param logicSentence Analizowane zdanie
	 * @return true jeżeli wyrażenie spełnione, false w przeciwnym wypadku
	 */
	public boolean analyze(LogicSentence logicSentence) {
		logger.info("Start analyze");
		boolean outcome = false;

		switch (logicSentence.getChosenPattern()) {
			case EXISTENCE:
				outcome = checkExistence(logicSentence);
				break;
		}

		logger.info("Finish analyze");
		return outcome;
	}

	/**
	 * Sprawdza wzorzec EXISTENCE - możliwość (gwarantowalność)
	 *
	 * @param logicSentence Analizowane zdanie
	 * @return true jeżeli wzorzec spełniony, false w przeciwnym wypadku
	 */
	private boolean checkExistence(LogicSentence logicSentence) {
		logger.info("Start checkExistence");

		switch (logicSentence.getChosenOperator()) {
			case EQ:
				for (String value : dataMap.get(logicSentence.getChosenVariable())) {
					if (value.equals(logicSentence.getChosenValue())) {
						return true;
					}
				}
				break;

			case NE:
				for (String value : dataMap.get(logicSentence.getChosenVariable())) {
					// NOT EQUALS TO NIE TO SAMO CO ABSENCE!
					if (!value.equals(logicSentence.getChosenValue())) {
						return true;
					}
				}
				break;
			default:
				logger.warn(
						"Warning in checkExistence, default switch option used! chosenOperator={}, chosenVariable={}, chosenValue={}",
						logicSentence.getChosenOperator(), logicSentence.getChosenVariable(), logicSentence.getChosenValue());
				break;
		}

		logger.info("Finish checkExistence");
		return false;
	}

	/**
	 * Sprawdza czy analizator jest gotowy do działania
	 *
	 * @return true jeżeli jest gotowy, false w przeciwnym wypadku
	 */
	public boolean isReady() {
		if (dataMap != null && dataHeaders != null && analyzerWorkType != null) {
			// TODO Dodać żeby sprawdzał te kolekcje też w głąb
			return true;
		} else {
			return false;
		}
	}

	public Map<String, ArrayList<String>> getDataMap() {
		return dataMap;
	}

	public List<String> getDataHeaders() {
		return dataHeaders;
	}

	public AnalyzerWorkType getAnalyzerWorkType() {
		return analyzerWorkType;
	}

	public void setAnalyzerWorkType(AnalyzerWorkType analyzerWorkType) {
		this.analyzerWorkType = analyzerWorkType;
	}
}
