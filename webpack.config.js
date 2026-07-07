const path = require('path');
const sass = require('sass');

module.exports = (env = {}) => {
    const outputDir = env.outputDir || process.env.WEBPACK_OUTPUT_DIR || 'target/classes/static';

    return {
        entry: './src/main/js/index.js',
        devtool: 'source-map',
        cache: {
            type: 'filesystem'
        },
        mode: 'development',
        output: {
            path: path.resolve(__dirname, outputDir),
            filename: 'bundle.js',
            clean: false
        },
        devServer: {
            static: {
                directory: __dirname
            },
            historyApiFallback: true,
            hot: true
        },
        resolve: {
            extensions: ['.js', '.jsx']
        },
        module: {
            rules: [
                {
                    test: /\.(js|jsx)$/,
                    include: path.resolve(__dirname, 'src/main/js'),
                    exclude: /(node_modules|bower_components|build)/,
                    use: [{
                        loader: 'babel-loader',
                        options: {
                            cacheDirectory: true,
                            presets: ['@babel/preset-env', '@babel/preset-react'],
                            plugins: [
                                ['@babel/plugin-proposal-class-properties'],
                                ['@babel/plugin-transform-runtime']
                            ]
                        }
                    }]
                },
                {
                    test: /\.(css)$/,
                    use: ['style-loader', 'css-loader']
                },
                {
                    test: /\.scss$/,
                    use: [
                        'style-loader',
                        'css-loader',
                        {
                            loader: 'sass-loader',
                            options: {
                                implementation: sass
                            }
                        }
                    ]
                },
                {
                    test: /\.svg$/,
                    use: [
                        {
                            loader: 'svg-url-loader',
                            options: {
                                limit: 10000
                            }
                        }
                    ]
                },
                {
                    test: /\.(png|jpg|gif)$/,
                    use: ['file-loader']
                }
            ]
        },
        performance: {
            maxAssetSize: 8 * 1024 * 1024,
            maxEntrypointSize: 8 * 1024 * 1024
        }
    };
};
