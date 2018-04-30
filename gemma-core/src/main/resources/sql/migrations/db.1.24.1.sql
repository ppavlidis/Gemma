-- data model revisions and cleanups. 4/2018

-- make retracted a field of BibliographicReference rather than a join
alter table BIBLIOGRAPHIC_REFERENCE
  add COLUMN RETRACTED TINYINT DEFAULT 0;
update BIBLIOGRAPHIC_REFERENCE b, PUBLICATION_TYPE p
set b.RETRACTED = 1
where p.BIBLIOGRAPHIC_REFERENCE_FK = b.ID and p.TYPE = "Retracted Publication";
drop table PUBLICATION_TYPE;

-- Remove unused audit events for ChromosomeFeature
delete a, t from AUDIT_EVENT a inner join CHROMOSOME_FEATURE c ON c.AUDIT_TRAIL_FK = a.AUDIT_TRAIL_FK
  inner join AUDIT_TRAIL t ON t.ID = a.AUDIT_TRAIL_FK;
alter table CHROMOSOME_FEATURE
  DROP COLUMN AUDIT_TRAIL_FK;

-- remove Investigators from Investigation
delete c, i from CONTACT c INNER JOIN INVESTIGATORS i ON i.INVESTIGATORS_FK = c.ID;
alter table INVESTIGATION
  drop column INVESTIGATORS_FK;
drop table INVESTIGATORS;

-- make more entities non-auditable
delete a, t from AUDIT_EVENT a inner join BIO_MATERIAL c ON c.AUDIT_TRAIL_FK = a.AUDIT_TRAIL_FK
  inner join AUDIT_TRAIL t ON t.ID = a.AUDIT_TRAIL_FK;
delete a, t from AUDIT_EVENT a inner join PROTOCOL c ON c.AUDIT_TRAIL_FK = a.AUDIT_TRAIL_FK
  inner join AUDIT_TRAIL t ON t.ID = a.AUDIT_TRAIL_FK;
delete a, t from AUDIT_EVENT a inner join EXTERNAL_DATABASE c ON c.AUDIT_TRAIL_FK = a.AUDIT_TRAIL_FK
  inner join AUDIT_TRAIL t ON t.ID = a.AUDIT_TRAIL_FK;
delete a, t from AUDIT_EVENT a inner join BIBLIOGRAPHIC_REFERENCE c ON c.AUDIT_TRAIL_FK = a.AUDIT_TRAIL_FK
  inner join AUDIT_TRAIL t ON t.ID = a.AUDIT_TRAIL_FK;
delete a, t from AUDIT_EVENT a inner join CONTACT c ON c.AUDIT_TRAIL_FK = a.AUDIT_TRAIL_FK
  inner join AUDIT_TRAIL t ON t.ID = a.AUDIT_TRAIL_FK;
delete a, t from AUDIT_EVENT a inner join BIO_ASSAY c ON c.AUDIT_TRAIL_FK = a.AUDIT_TRAIL_FK
  inner join AUDIT_TRAIL t ON t.ID = a.AUDIT_TRAIL_FK;
delete a, t from AUDIT_EVENT a inner join TREATMENT c ON c.AUDIT_TRAIL_FK = a.AUDIT_TRAIL_FK
  inner join AUDIT_TRAIL t ON t.ID = a.AUDIT_TRAIL_FK;

alter table CHARACTERISTIC
  drop FOREIGN KEY CHARACTERISTIC_AUDIT_TRAIL_FKC;
delete a, t from AUDIT_EVENT a inner join CHARACTERISTIC c ON c.AUDIT_TRAIL_FK = a.AUDIT_TRAIL_FK
  inner join AUDIT_TRAIL t ON t.ID = a.AUDIT_TRAIL_FK;

alter table ANALYSIS
  drop FOREIGN KEY AUDITABLE_AUDIT_TRAIL_FKC;
delete a, t from AUDIT_EVENT a inner join ANALYSIS c ON c.AUDIT_TRAIL_FK = a.AUDIT_TRAIL_FK
  inner join AUDIT_TRAIL t ON t.ID = a.AUDIT_TRAIL_FK;


alter table BIO_MATERIAL
  DROP COLUMN AUDIT_TRAIL_FK;
alter table PROTOCOL
  DROP COLUMN AUDIT_TRAIL_FK;
alter table ANALYSIS
  DROP COLUMN AUDIT_TRAIL_FK;
alter table EXTERNAL_DATABASE
  DROP COLUMN AUDIT_TRAIL_FK;
alter table BIBLIOGRAPHIC_REFERENCE
  DROP COLUMN AUDIT_TRAIL_FK;
alter table CONTACT
  DROP COLUMN AUDIT_TRAIL_FK;
alter table BIO_ASSAY
  DROP COLUMN AUDIT_TRAIL_FK;
alter table TREATMENT
  DROP COLUMN AUDIT_TRAIL_FK;
alter table CHARACTERISTIC
  DROP COLUMN AUDIT_TRAIL_FK;

-- remove more unneded tables
drop table CYTOGENETIC_LOCATION; -- not in data model
drop table CHARTEMP; -- tmp
drop table PARAMETERIZABLE; -- not in data model
drop table PROTOCOL_APPLICATION; -- not in data model
drop table TMP_GENES_TO_REMOVE; -- tmp
drop table TMP_GENE_PRODUCTS_TO_REMOVE; -- tmp
drop table PUBLICATION_TYPE; -- removed from data model

-- removed from data model
alter table BIO_ASSAY
  DROP COLUMN RAW_DATA_FILE_FK;
alter table INVESTIGATION
  DROP COLUMN RAW_DATA_FILE_FK;
alter table BIBLIOGRAPHIC_REFERENCE
  DROP COLUMN FULL_TEXT_P_D_F_FK,
  DROP COLUMN FULL_TEXT_PDF_FK;
drop table SOURCE_FILES;
drop table LOCAL_FILE;

alter table TREATMENT
  drop FOREIGN KEY TREATMENT_ACTION_FKC;
delete c from CHARACTERISTIC c inner join TREATMENT t ON t.ACTION_FK = c.ID;
alter table TREATMENT
  DROP COLUMN ACTION_FK,
  DROP COLUMN ACTION_MEASUREMENT_FK;

-- Misc
alter table CROMOSOME_FEATURE
  DROP COLUMN TYPE,
  DROP COLUMN CDS_PHYSICAL_LOCATION_FK,
  DROP COLUMN METHOD,
  DROP COLUMN CYTOGENIC_LOCATION; -- removed from data model
