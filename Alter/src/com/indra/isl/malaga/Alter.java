package com.indra.isl.malaga;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Clase principal de la aplicación
 *
 * @author ajifernandez
 *
 */
public class Alter {

	public static String configSource = "";
	public static String configTarget = "";
	public static String replacementSource = "";

	private static final String CONFIG = "/config";
	static Map<String, String> replacementUsedMap;

	public static void main(String[] args) {
		if (args.length == 3) {
			configSource = args[0];
			configTarget = args[1];
			replacementSource = args[2];

			Map<String, String> replacementMap = getReplacementMap(replacementSource);
			replacementUsedMap = getReplacementMap(replacementSource);

			File configTargetFile = new File(configTarget + CONFIG);
			String[] extensions = new String[] { "conf", "xml", "properties" };

			try {
				// Borramos destino
				FileUtils.deleteDirectory(configTargetFile);

				// Copiar datos de configuración a configTarget
				FileUtils.copyDirectory(new File(configSource), configTargetFile);
				Iterator<File> iterateFiles = FileUtils.iterateFiles(configTargetFile, extensions, true);
				while (iterateFiles.hasNext()) {
					File file = iterateFiles.next();
					System.out.println(file.getName());
					if (file.isFile()) {
						List<String> readLines = FileUtils.readLines(file);

						List<String> writeLines = new ArrayList<String>();
						for (String line : readLines) {
							writeLines.add(replace(file, line, replacementMap));
						}
						FileUtils.writeLines(file, writeLines);

					}

				}

				// Modificamos el fichero de log, para que mostrar las trazas
				// por consola
				List<String> readLines = new ArrayList<String>();
				File loggingFile = null;
				try {
					// Servidor
					loggingFile = FileUtils.getFile(configTarget + CONFIG + "/logging.properties");
					readLines = FileUtils.readLines(loggingFile);
				} catch (FileNotFoundException e) {
					loggingFile = FileUtils.getFile(configTarget + CONFIG + "/log/log.properties");
				}

				try {
					// cliente
					loggingFile = FileUtils.getFile(configTarget + CONFIG + "/log/log.properties");
					readLines = FileUtils.readLines(loggingFile);
				} catch (FileNotFoundException e) {
					loggingFile = FileUtils.getFile(configTarget + CONFIG + "/logging.properties");
				}

				List<String> writeLines = new ArrayList<String>();
				for (String line : readLines) {
					if (line.startsWith("handlers") && line.contains("logging.FileHandler")) {
						writeLines.add("handlers= java.util.logging.FileHandler, java.util.logging.ConsoleHandler");
					} else if (line.startsWith("handlers") && line.contains("GZipFileHandler")) {
						writeLines
							.add("handlers=com.indra.davinci.common.log.GZipFileHandler, java.util.logging.ConsoleHandler");
					} else {
						writeLines.add(line);
					}

				}

				FileUtils.writeLines(loggingFile, writeLines);

			} catch (IOException e) {
				e.printStackTrace();
			}

			System.out.println("##########################");
			System.out.println("Claves que sobran");
			if (replacementUsedMap.keySet().size() == 0) {
				System.out.println("Enhorabuena, no te sobran claves en el replacements");
			} else {
				for (String key : replacementUsedMap.keySet()) {
					System.out.println(key);
				}
			}
		} else {
			System.err.println("Número de argumentos inválidos Alter");
			System.err.println("args[0] = configuration_repo_server");
			System.err.println("args[1] = _configuration_gen_SGC_SERVER");
			System.err.println("args[2] = _my_replacements/replacements_server/replacements.properties");
		}
	}

	/**
	 * Realiza el reemplazo de los replacements
	 *
	 * @param file
	 * @param line
	 * @param replacementMap
	 */
	private static String replace(File file, String line, Map<String, String> replacementMap) {

		// System.out.println("ct:"+configTarget);
		String routeName = new String(file.getAbsolutePath().replace(configTarget, "").replace("\\", ".")
			.replace("/", "."));
		// System.out.println("rn:"+routeName);
		if (routeName.startsWith(".")) {
			routeName = routeName.substring(1);
		}
		routeName = routeName.substring(routeName.indexOf("config"));
		// System.out.println(routeName);

		List<String> patterns = getPatterns(line);
		for (String pattern : patterns) {
			String completRouteName = routeName + "." + pattern;

			String realValue = replacementMap.get(completRouteName);
			if (realValue != null) {
				line = line.replace("${" + pattern + "}", realValue);
				replacementUsedMap.remove(completRouteName);
			} else {
				System.err.println("No hay reemplazo para la variable " + pattern + "\n en el fichero "
					+ file.getAbsolutePath());
			}
		}

		return line;
	}

	/**
	 * Obtenemos los patrones de la línea
	 *
	 * @param line
	 * @return
	 */
	private static List<String> getPatterns(String line) {
		List<String> patterns = new ArrayList<String>();

		if (!"};".equals(line)) {
			String[] splitByWholeSeparator = StringUtils.splitByWholeSeparator(line, "${");
			for (int i = 0; i < splitByWholeSeparator.length; i++) {
				String nextElement = splitByWholeSeparator[i];
				if (nextElement.contains("}") && splitByWholeSeparator.length > 1) {
					String var = nextElement.substring(0, nextElement.indexOf("}"));
					patterns.add(var);
				}
			}
		}
		return patterns;
	}

	/**
	 * Obtenemos el mapa de replacements
	 *
	 * @param replacementSource
	 * @return
	 */
	private static Map<String, String> getReplacementMap(String replacementSource) {
		Map<String, String> replacementMap = new HashMap<String, String>();

		try {
			List<String> readLines = FileUtils.readLines(new File(replacementSource));
			for (String line : readLines) {
				if (!line.startsWith("#") && !"".equals(line)) {
					String[] lineSplit = line.split("=");
					replacementMap.put(lineSplit[0], lineSplit.length > 1 ? lineSplit[1] : "");
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
		return replacementMap;
	}
}
