drop trigger if exists insert_authorisation_prison_absence_categorisation on tap.authorisation;
drop trigger if exists update_authorisation_prison_absence_categorisation on tap.authorisation;
drop trigger if exists insert_occurrence_prison_absence_categorisation on tap.occurrence;
-- typo on the original creation of the trigger
drop trigger if exists update_authorisation_prison_absence_categorisation on tap.occurrence;
drop function if exists tap.merge_prison_absence_categorisation;
drop table if exists tap.prison_absence_categorisation;
