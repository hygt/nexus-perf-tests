# Peformance tests for nexus v1


In order to set up the tests it's necessary to set up the projects. It can be done using `scripts/schemas.sh` or `scripts/schemas-single-project.sh`.
The first script is puts all the schemas in a single project and uses CrossProjectResolver to resolve them. The second puts the schemas in each project. Currently the second script is recommended due to performance concerns for cross-project resolution.
The scripts accepts the following parameter:
```
-a admin service base URL(including v1 prefix)
-k KG base URL (including v1 prefix)
-t token - token of a user who has permission to create organizations/projects/resources
-n number of projects to create 
```

Running this script will create projects in organization `perftestorg` with names `perftestproj${i}` where `$i` is project number starting from 1.