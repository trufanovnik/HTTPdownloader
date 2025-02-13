# HTTP-загрузчик
## Задача
- [Источник](https://habr.com/ru/companies/ecwid/articles/315228/)

Программа считывает ссылки на репозитории GitHub из файла [repositories](https://github.com/trufanovnik/HTTPdownloader/blob/main/src/main/resources/repositories.txt). 
Используя [GitHub API](https://docs.github.com/ru/rest?apiVersion=2022-11-28) получает ветку по умолчанию и составляет ссылку для скачивания репозитория в папку [downloaded_files](https://github.com/trufanovnik/HTTPdownloader/tree/main/src/main/resources/downloaded_files).

Загрузка нескольких файлов одновременно осуществлена с использованием [Semaphore](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/Semaphore.html).
Согласно требованию задачи, загрузка осуществляется с определенным ограничением скорости закачки (по факту - сохранения из InputStream, но это также косвенно ограничивает поступление данных от сервера благодаря протоколам TCP).
