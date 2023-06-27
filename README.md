Search Engine Final Work Skillbox

This is the final project for the "Java Developer from Scratch" course offered by Skillbox. The goal of the project is to create a search engine application that can index website pages and perform fast searches.

The search engine application is built with Spring and runs on any server or computer. It uses a MySQL database to store data and has a user-friendly web interface and an API for managing and retrieving search results.

The search engine works according to the following principles:

The application reads the website addresses to be indexed from a configuration file.
The search engine automatically traverses all the pages on the specified websites and indexes them (creates an index) so that the most relevant pages can be found quickly for any search query.
Users send search queries through the search engine's API. A search query is a set of words that need to be found on the website's pages.
The search query is transformed into a list of words in their base form in a specific way.
The search engine searches the index for pages that contain all of these words.
The search results are ranked, sorted, and returned to the user.
In summary, this project aims to create a powerful search engine that provides fast and accurate search results for users, based on an index of website pages that it automatically creates and updates.