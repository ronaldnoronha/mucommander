/*
 * This file is part of muCommander, http://www.mucommander.com
 *
 * muCommander is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * muCommander is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.mucommander.search;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mucommander.commons.file.AbstractFile;

/**
 * This job executes a file search.
 *
 * @author Ronald Noronha
 */
public class SearchJob {
    private static final Logger LOGGER = LoggerFactory.getLogger(SearchJob.class);

    private List<AbstractFile> entrypoints;
    private Predicate<AbstractFile> fileMatcher;
    private Predicate<AbstractFile> browseMatcher;
    private List<AbstractFile> findings;

    SearchJob() {
        findings = new CopyOnWriteArrayList<>();
    }

    void setEntrypoints(List<AbstractFile> entrypoints) {
        this.entrypoints = entrypoints;
    }

    public void search() {
        LOGGER.info("start searching {}", entrypoints);
        List<AbstractFile> files = entrypoints;
        for (int i=0; i<3; i++) {
            if (files.isEmpty())
                break;
            files = search(files);
        }
    }

    public void setFileMatcher(Predicate<AbstractFile> fileMatcher) {
        this.fileMatcher = fileMatcher;
    }

    public void setBrowseMatcher(Predicate<AbstractFile> browseMatcher) {
        this.browseMatcher = browseMatcher;
    }

    private List<AbstractFile> search(List<AbstractFile> files) {
        return files.parallelStream()
                .filter(browseMatcher)
                .map(this::search)
                .flatMap(stream -> stream)
                .collect(Collectors.toList());
    }

    private Stream<AbstractFile> search(AbstractFile file) {
        AbstractFile[] children;
        try {
            children = file.ls();
        } catch (IOException e) {
            LOGGER.info("failed to list: " + file);
            return Stream.empty();
        }
        examine(children);
        return Stream.of(children);
    }

    private void examine(AbstractFile[] files) {
        if (files.length == 0)
            return;
        List<AbstractFile> passed = Stream.of(files)
                .filter(fileMatcher)
                .collect(Collectors.toList());
        if (!passed.isEmpty()) {
            LOGGER.info("found: " + passed);
            findings.addAll(passed);
        }
    }

    public List<AbstractFile> getFindings() {
        LOGGER.info("get results: " + findings.size());
        return findings;
    }
}