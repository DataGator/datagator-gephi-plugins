/*
 * Copyright 2015 University of Denver
 * Author(s) : LIU Yu <liuyu@opencps.net>
 * Website : http://github.com/DataGator/gephi-plugins
 *
 * This file is part of DataGator Gephi Plugins.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2015 University of Denver. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 3 only ("GPL") or the Common Development and
 * Distribution License("CDDL") (collectively, the "License"). You may not use
 * this file except in compliance with the License. You can obtain a copy of
 * the License at /cddl-1.0.txt and /gpl-3.0.txt. See the License for the
 * specific language governing permissions and limitations under the License.
 * When distributing the software, include this License Header Notice in each
 * file and include the License files at /cddl-1.0.txt and /gpl-3.0.txt.
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 *
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 3, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 3] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 3 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 3 code and therefore, elected the GPL
 * Version 3 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 *
 * Contributor(s):
 *
 */
package org.datagator.ext.gephi.importer;

import org.gephi.io.importer.api.FileType;
import org.gephi.io.importer.spi.FileImporter;
import org.gephi.io.importer.spi.FileImporterBuilder;
import org.openide.filesystems.FileObject;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

/**
 * FileImporterBuilder implementation.
 *
 * The builder is responsible for creating instances of the importer.
 *
 * @author LIU Yu <liuyu@opencps.net>
 */
@ServiceProvider(service = FileImporterBuilder.class)
public class MatrixJsonImporterBuilder
    implements FileImporterBuilder
{

    public static final String IDENTIFER = "json";

    @Override
    public FileImporter buildImporter()
    {
        return (FileImporter) new MatrixJsonImporter();
    }

    @Override
    public String getName()
    {
        return IDENTIFER;
    }

    @Override
    public FileType[] getFileTypes()
    {
        String file_info = NbBundle.getMessage(MatrixJsonImporter.class,
            "MatrixJsonImporter.text.file_info");
        FileType ft = new FileType(".json", file_info);
        return new FileType[]{ft};
    }

    @Override
    public boolean isMatchingImporter(FileObject fileObject)
    {
        return fileObject.getExt().equalsIgnoreCase(IDENTIFER);
    }
}
