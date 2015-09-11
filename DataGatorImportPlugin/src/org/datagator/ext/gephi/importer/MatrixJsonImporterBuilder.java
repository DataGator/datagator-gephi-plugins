/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.datagator.ext.gephi.importer;

import org.gephi.io.importer.api.FileType;
import org.gephi.io.importer.spi.FileImporter;
import org.gephi.io.importer.spi.FileImporterBuilder;
import org.openide.filesystems.FileObject;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

/**
 * FileImporterBuilder implementation for the DataGator Matrix JSON format. The
 * builder is responsible for creating instances of the importer.
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
