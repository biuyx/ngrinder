package org.ngrinder.script.handler;

import static org.ngrinder.common.util.CollectionUtils.newArrayList;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.maven.cli.MavenCli;
import org.ngrinder.common.util.PropertiesWrapper;
import org.ngrinder.model.User;
import org.ngrinder.script.model.FileEntry;
import org.ngrinder.script.model.FileType;
import org.springframework.stereotype.Component;

@Component
public class GroovyMavenProjectScriptHandler extends GroovyScriptHandler {
	public GroovyMavenProjectScriptHandler() {
		super("Groovy Maven Project");
	}

	private static final String SRC_MAIN_RESOURCES = "/src/main/resources";
	private static final String SRC_MAIN_JAVA = "/src/main/java";

	@Override
	public boolean canHandle(FileEntry fileEntry) {
		if (fileEntry.getCreatedUser() == null) {
			return false;
		}
		String path = fileEntry.getPath();
		if (!path.contains(SRC_MAIN_JAVA) || !FilenameUtils.isExtension(path, "groovy")) {
			return false;
		}

		return fileEntryRepository
				.hasFileEntry(fileEntry.getCreatedUser(), path.substring(0, path.lastIndexOf(SRC_MAIN_JAVA)) + "/pom.xml");
	}

	@Override
	protected Integer order() {
		return 200;
	}

	@Override
	public List<FileEntry> getLibAndResourceEntries(User user, FileEntry scriptEntry, long revision) {
		List<FileEntry> fileList = newArrayList();
		for (FileEntry eachFileEntry : fileEntryRepository.findAll(user, getBasePath(scriptEntry) + "/src/main/resources/", revision)) {
			FileType fileType = eachFileEntry.getFileType();
			if (fileType.isResourceDistributable()) {
				fileList.add(eachFileEntry);
			}
		}

		for (FileEntry eachFileEntry : fileEntryRepository.findAll(user, getBasePath(scriptEntry) + SRC_MAIN_JAVA, revision)) {
			FileType fileType = eachFileEntry.getFileType();
			if (fileType.isLibDistribtable()) {
				fileList.add(eachFileEntry);
			}
		}
		return fileList;
	}

	@Override
	protected String calcDistSubPath(String basePath, FileEntry each) {
		String calcDistSubPath = calcDistSubPath(basePath, each);
		if (calcDistSubPath.startsWith(SRC_MAIN_JAVA)) {
			return calcDistSubPath.substring(SRC_MAIN_JAVA.length());
		} else if (calcDistSubPath.startsWith(SRC_MAIN_RESOURCES)) {
			return calcDistSubPath.substring(SRC_MAIN_RESOURCES.length());
		}
		return calcDistSubPath;
	}

	@Override
	protected void prepareDistMore(String identifier, User user, FileEntry script, File distDir, PropertiesWrapper properties) {
		String pomPathInSVN = getBasePath(script) + "/pom.xml";
		File pomFile = new File(distDir, "pom.xml");
		fileEntryRepository.writeContentTo(user, pomPathInSVN, pomFile);
		MavenCli cli = new MavenCli();
		ByteArrayOutputStream writer = new ByteArrayOutputStream();
		cli.doMain(new String[] { "dependency:copy-dependencies", "-DoutputDirectory=./lib" }, distDir.getAbsolutePath(), new PrintStream(
				writer), new PrintStream(writer));
		LOGGER.info("Files in {} is copied into {}/lib folder", pomPathInSVN, distDir.getAbsolutePath());
		LOGGER.info(writer.toString());
	}

	@Override
	protected String getBasePath(FileEntry script) {
		String path = script.getPath();
		return path.substring(0, path.lastIndexOf(SRC_MAIN_JAVA));
	}

}