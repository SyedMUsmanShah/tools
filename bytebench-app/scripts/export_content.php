<?php
/*
 * Builds app/src/main/assets/content.json from the Bytebench Laravel project:
 * site identity, about page, services, case studies, blog posts, free tools,
 * legal pages and booking hours. It loads the project's config files and
 * seeders against tiny stand-ins for Laravel, so no database or Composer
 * install is needed.
 *
 *   php scripts/export_content.php /path/to/bytebench app/src/main/assets/content.json
 */

namespace {
    $root = rtrim($argv[1] ?? '', '/');
    $out = $argv[2] ?? null;
    if (!$root || !$out || !is_dir("$root/config")) {
        fwrite(STDERR, "usage: php export_content.php <laravel-project> <out.json>\n");
        exit(1);
    }

    function env($key, $default = null) { return $default; }
    function now() { return gmdate('c'); }

    function slugify(string $s): string
    {
        $s = strtolower(trim(preg_replace('/[^A-Za-z0-9]+/', '-', iconv('UTF-8', 'ASCII//TRANSLIT', $s)), '-'));
        return $s;
    }

    $GLOBALS['captured'] = ['services' => [], 'projects' => [], 'posts' => []];
}

namespace Illuminate\Support {
    class Str
    {
        public static function slug($s) { return \slugify($s); }
    }
}

namespace Illuminate\Database {
    class Seeder
    {
        public function call($class) { (new $class)->run(); }
    }
}

namespace App\Models {
    class Query
    {
        public function first() { return null; }
    }

    abstract class StubModel
    {
        public bool $exists = false;
        public array $attributes = [];

        public function __construct(array $a = []) { $this->attributes = $a; }
        public static function where(...$args) { return new Query(); }
        public function fill(array $a) { $this->attributes = $a + $this->attributes; return $this; }
        public function isCaseStudy() { return false; }
    }

    class Service extends StubModel
    {
        public static function firstOrCreate(array $key, array $values)
        {
            $GLOBALS['captured']['services'][$key['slug']] = $key + $values;
        }
    }

    class Post extends StubModel
    {
        public static function firstOrCreate(array $key, array $values)
        {
            $GLOBALS['captured']['posts'][$key['slug']] = $key + $values;
        }
    }

    class Project extends StubModel
    {
        public function save()
        {
            $a = $this->attributes;
            $a['slug'] = $a['slug'] ?? \slugify($a['title']);
            $GLOBALS['captured']['projects'][$a['slug']] = $a;
        }
    }

    class Tool extends StubModel {}
    class User extends StubModel
    {
        public static function updateOrCreate(...$args) {}
    }
}

namespace {
    $site = require "$root/config/site.php";
    $free = require "$root/config/freetools.php";

    foreach (['ServiceSeeder', 'CaseStudySeeder', 'ServicesUpdateSeeder'] as $s) {
        require_once "$root/database/seeders/$s.php";
    }
    (new Database\Seeders\ServiceSeeder)->run();
    (new Database\Seeders\CaseStudySeeder)->run();
    (new Database\Seeders\ServicesUpdateSeeder)->run();

    /** Turns the simple HTML used in the legal pages into Markdown. */
    function html_to_md(string $html): string
    {
        $md = $html;
        $md = preg_replace_callback('#<a [^>]*href="([^"]*)"[^>]*>(.*?)</a>#s', fn ($m) => $m[1] === strip_tags($m[2]) ? $m[1] : '['.strip_tags($m[2]).']('.$m[1].')', $md);
        $md = preg_replace('#<(strong|b)>(.*?)</\1>#s', '**$2**', $md);
        $md = preg_replace('#<(em|i)>(.*?)</\1>#s', '*$2*', $md);
        $md = preg_replace('#<h2[^>]*>(.*?)</h2>#s', "\n\n## $1\n\n", $md);
        $md = preg_replace('#<h3[^>]*>(.*?)</h3>#s', "\n\n### $1\n\n", $md);
        $md = preg_replace('#<li[^>]*>(.*?)</li>#s', "\n- $1", $md);
        $md = preg_replace('#</?(ul|ol)[^>]*>#', "\n", $md);
        $md = preg_replace('#<p[^>]*>(.*?)</p>#s', "\n\n$1\n\n", $md);
        $md = preg_replace('#<br\s*/?>#', "\n", $md);
        $md = strip_tags($md);
        $md = html_entity_decode($md, ENT_QUOTES | ENT_HTML5, 'UTF-8');
        $md = preg_replace("/[ \t]+\n/", "\n", $md);
        $md = preg_replace("/\n{3,}/", "\n\n", $md);
        return trim($md);
    }

    /** Renders one of the legal Blade views: evaluates its @php block and {{ }} echoes. */
    function render_legal(string $file, array $site): string
    {
        $src = file_get_contents($file);
        $vars = ['site' => $site['name'], 'email' => $site['owner']['email'], 'owner' => $site['owner']['name']];
        $config = function ($key) use ($site) {
            $v = ['site' => $site];
            foreach (explode('.', $key) as $k) { $v = $v[$k] ?? null; }
            return $v;
        };
        $src = preg_replace('/^@(extends|section|endsection|php|endphp)\b.*$/m', '', $src);
        $src = preg_replace_callback('/\{\{\s*(.*?)\s*\}\}/s', function ($m) use ($vars, $config) {
            $e = trim($m[1]);
            if (preg_match('/^\$(\w+)$/', $e, $v)) return $vars[$v[1]] ?? '';
            if (preg_match("/^config\('([^']+)'\)$/", $e, $v)) return (string) $config($v[1]);
            if (preg_match("/^url\('([^']*)'\)$/", $e, $v)) return $v[1] === '/' ? 'the '.$config('site.name').' website' : $v[1];   // no live domain yet
            if (preg_match("/^route\('([^']+)'\)$/", $e, $v)) return '/'.str_replace('.', '/', $v[1]);
            return '';
        }, $src);
        return html_to_md($src);
    }

    $legalTitles = [
        'privacy-policy' => 'Privacy Policy',
        'terms' => 'Terms and Conditions',
        'disclaimer' => 'Disclaimer',
        'cookie-policy' => 'Cookie Policy',
        'refund-policy' => 'Refund Policy',
    ];
    $legal = [];
    foreach ($legalTitles as $slug => $title) {
        $legal[] = ['slug' => $slug, 'title' => $title, 'body' => render_legal("$root/resources/views/legal/$slug.blade.php", $site)];
    }

    $lines = fn ($s) => array_values(array_filter(array_map('trim', preg_split('/\r?\n/', $s ?? ''))));

    $services = [];
    $i = 0;
    foreach ($GLOBALS['captured']['services'] as $s) {
        $services[] = [
            'slug' => $s['slug'], 'title' => $s['title'], 'icon' => $s['icon'] ?? 'fa-solid fa-briefcase',
            'summary' => $s['summary'], 'includes' => $lines($s['includes'] ?? ''), 'details' => $s['details'] ?? '',
            'priceLabel' => $s['price_label'] ?? null, 'sortOrder' => ++$i,
        ];
    }

    $projects = [];
    foreach ($GLOBALS['captured']['projects'] as $p) {
        $projects[] = [
            'slug' => $p['slug'], 'title' => $p['title'], 'summary' => $p['summary'] ?? '',
            'role' => $p['role'] ?? null, 'year' => $p['year'] ?? null,
            'tech' => array_values(array_filter(array_map('trim', explode(',', $p['tech'] ?? '')))),
            'url' => $p['url'] ?? null, 'image' => null,
            'challenge' => $p['challenge'] ?? '', 'approach' => $p['approach'] ?? '', 'outcome' => $p['outcome'] ?? '',
        ];
    }

    $posts = [];
    foreach ($GLOBALS['captured']['posts'] as $p) {
        $posts[] = [
            'slug' => $p['slug'], 'title' => $p['title'], 'category' => $p['category'] ?? null,
            'excerpt' => $p['excerpt'] ?? '', 'body' => $p['body'], 'cover' => null,
            'publishedAt' => gmdate('Y-m-d'),
            'readingMinutes' => max(1, (int) ceil(str_word_count(strip_tags($p['body'])) / 220)),
        ];
    }

    $tools = [];
    foreach ($free['tools'] as $slug => $t) {
        $pitch = $free['tool_pitches'][$slug] ?? $free['pitches'][$t['category']] ?? null;
        $tools[] = [
            'slug' => $slug, 'name' => $t['name'], 'category' => $t['category'], 'icon' => $t['icon'],
            'summary' => $t['summary'], 'steps' => $t['steps'] ?? [], 'about' => $t['about'] ?? '',
            'faqs' => array_map(fn ($f) => ['q' => $f[0], 'a' => $f[1]], $t['faqs'] ?? []),
            'pitch' => $pitch, 'topics' => $free['topics'][$slug] ?? [],
        ];
    }

    $o = $site['owner'];
    $content = [
        'version' => 1,
        'generatedAt' => gmdate('c'),
        'site' => [
            'name' => $site['name'], 'tagline' => $site['tagline'], 'description' => $site['description'],
            'owner' => [
                'name' => $o['name'], 'shortName' => $o['short_name'], 'role' => $o['role'], 'location' => $o['location'],
                'email' => $o['email'], 'whatsapp' => $o['whatsapp'], 'whatsappDisplay' => $o['whatsapp_display'],
                'whatsappMessage' => $o['whatsapp_message'],
            ],
            'socials' => array_values(array_map(
                fn ($name, $url) => ['name' => $name, 'url' => $url, 'icon' => $site['social_icons'][$name] ?? 'fa-solid fa-link'],
                array_keys(array_filter($site['socials'])), array_filter($site['socials'])
            )),
            'intro' => $site['intro'],
            'experience' => $site['experience'],
            'skills' => array_map(fn ($group, $items) => ['group' => $group, 'items' => $items], array_keys($site['skills']), $site['skills']),
            'education' => $site['education'],
            'stats' => array_map(fn ($s) => [
                'label' => $s['label'], 'since' => $s['since'] ?? null, 'value' => $s['value'] ?? null,
                'count' => $s['count'] ?? null, 'suffix' => $s['suffix'] ?? '',
            ], $site['stats']),
            'steps' => $site['steps'],
            'booking' => $site['booking'],
            'authorBio' => $site['author_bio'],
            'blogCategories' => $site['blog_categories'],
            'legalUpdated' => $site['legal_updated'],
            'defaultSkin' => $site['skin'],
        ],
        'toolCategories' => array_map(fn ($k, $v) => ['slug' => $k, 'name' => $v], array_keys($free['categories']), $free['categories']),
        'featuredTools' => $free['default_featured'],
        'tools' => $tools,
        'services' => $services,
        'projects' => $projects,
        'posts' => $posts,
        'products' => [],
        'legal' => $legal,
    ];

    @mkdir(dirname($out), 0777, true);
    file_put_contents($out, json_encode($content, JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES)."\n");
    fwrite(STDERR, sprintf("wrote %s: %d tools, %d services, %d projects, %d posts, %d legal pages\n",
        $out, count($tools), count($services), count($projects), count($posts), count($legal)));
}
