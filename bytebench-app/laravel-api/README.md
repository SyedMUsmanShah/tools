# Bytebench website → app sync

The Android app ships with your current content built in. Once the website is live,
add this small, read-only endpoint so the app can pick up new posts, services,
products and free-tool settings from your admin panel.

## Install (2 steps)

1. Copy `app/Http/Controllers/AppContentController.php` into the same folder of your
   Laravel project.
2. Add this line to `routes/web.php`, **above** the `Route::get('/{page}', …)` legal-pages route:

   ```php
   Route::get('/api/app/content.json', \App\Http\Controllers\AppContentController::class)->name('app.content');
   ```

Then run `php artisan optimize:clear` and open `https://your-domain/api/app/content.json`
in a browser — you should see JSON.

## Connect the app

In the app: **More → Settings & theme → Website sync**, enter your website address
(for example `https://yourdomain.com`) and tap **Save & sync now**.

## Notes

- Only published, public content is included. Contact messages, purchase requests,
  call bookings, subscribers and admin settings are never sent.
- The response is cached for 10 minutes. After editing something, wait a few minutes
  or run `php artisan cache:forget app_content_json`.
- Cover images and project images are loaded from `/storage/…` on your site.
