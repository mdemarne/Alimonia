Alimonia
========

Alimonia is a web-based platform for recipe retrieval and fridge management.

The project is based on the Play Framework version 2.0.4 and uses the jdbc MySQL driver. Unfortunately the platform is not compatible with the newer versions of Play for now.

### Basic functionality

- Fridge management by sections and quantities
- Retrieval of recipes based on the fridge content and a list of potential items to buy
- Retrieval of recipes by name
- Addition of recipes (list of ingredients, description, name, picture)
- Ranking of recipes (like / dislike buttons)

### Development

The project was build for the class Web Application (15-437/15-637), Carnegie Mellon University, PA. This project was build by a group of two students.

### Javascript tools

The platform uses TinyMCE to edit descriptions, which is included in `public/`.

### Run it

The database settings are left blank. They can be edited [here](https://github.com/mdemarne/Alimonia/blob/master/conf/application.conf). The drivers are included and can be changed. The Evolution system supported by Play will automatically populate the database.
